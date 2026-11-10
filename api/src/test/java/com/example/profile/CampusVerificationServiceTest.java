package com.example.profile;

import com.example.campus.*;
import com.example.exception.BusinessException;
import com.example.mapper.*;
import com.example.pojo.vo.UserProfileVO;
import com.example.service.*;
import com.example.utils.UserContext;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampusVerificationServiceTest {
    UserProfileService profiles; UserProfileMapper users; CampusVerificationMapper records;
    CampusVerificationProvider provider; CampusVerificationService service;
    UserProfileVO profile(String school,String number) { return new UserProfileVO(2,"student","同学",null,null,null,null,school,"计算机学院",number,"USER"); }
    CampusVerificationRecord record(String status) {
        var r=new CampusVerificationRecord();r.setId(10L);r.setBaseUserId(2);r.setRequestId("request-test");
        r.setSchool("校园大学");r.setStudentNumber("20260001");r.setStatus(status);r.setProvider("school");
        r.setExpiresAt(LocalDateTime.now().plusDays(30));return r;
    }
    @BeforeEach void setup() {
        UserContext.setUser(2,"USER");
        profiles=mock(UserProfileService.class);users=mock(UserProfileMapper.class);records=mock(CampusVerificationMapper.class);provider=mock(CampusVerificationProvider.class);
        service=new CampusVerificationService(profiles,users,records,provider);
        when(profiles.mine()).thenReturn(profile("校园大学","20260001"));when(users.lockActiveUser(2)).thenReturn(2);
        when(provider.available()).thenReturn(true);when(provider.key()).thenReturn("school");when(records.complete(any())).thenReturn(1);
    }
    @AfterEach void clearIdentity() { UserContext.clear(); }
    @Test void missingOrWrongRoleCannotLockOrApply() { UserContext.clear();error(()->service.apply(),403);UserContext.setUser(1,"ADMIN");error(()->service.refresh(),403);verifyNoInteractions(users,records,provider); }
    @Test void locksBeforeReadingSavedProfileToAvoidRepeatableReadSnapshotRace() { service.apply();var order=inOrder(users,profiles);order.verify(users).lockActiveUser(2);order.verify(profiles).mine(); }
    BusinessException error(Runnable action,int code) { var e=assertThrows(BusinessException.class,action::run);assertEquals(code,e.getCode());return e; }
    @Test void disconnectedProviderNeverCreatesOrChecksRecords() {
        when(provider.available()).thenReturn(false);assertFalse(service.mine().available());assertEquals("UNVERIFIED",service.mine().status());
        error(()->service.apply(),503);error(()->service.refresh(),503);verify(records,never()).insert(any());verify(provider,never()).check(any());
    }
    @Test void inactiveOrMissingIdentityRejectedBeforeAnyRecordAccess() {
        when(profiles.mine()).thenThrow(new BusinessException("账号失效",403));error(()->service.mine(),403);error(()->service.apply(),403);error(()->service.refresh(),403);verifyNoInteractions(records,provider);
    }
    @Test void concurrentAccountDisableIsRejected() { when(users.lockActiveUser(2)).thenReturn(null);error(()->service.apply(),403);verify(records,never()).insert(any()); }
    @Test void incompleteSavedCampusDataCannotApply() { when(profiles.mine()).thenReturn(profile(null,"2026"));error(()->service.apply(),400);verify(records,never()).insert(any()); }
    @Test void applySnapshotsSavedDataAndDoesNotCallExternalServiceInInsertTransaction() {
        when(records.insert(any())).thenAnswer(i->{var r=i.getArgument(0,CampusVerificationRecord.class);when(records.latest(2)).thenReturn(r);return 1;});
        assertEquals("PENDING",service.apply().status());
        verify(records).insert(argThat(r->r.getBaseUserId()==2&&r.getSchool().equals("校园大学")&&r.getStudentNumber().equals("20260001")&&r.getRequestId().length()==36));
        verify(provider,never()).check(any());
    }
    @Test void repeatedPendingApplyIsIdempotent() { when(records.latest(2)).thenReturn(record("PENDING"));assertEquals("PENDING",service.apply().status());verify(records,never()).insert(any()); }
    @Test void currentVerifiedResultCannotBeOverwrittenByNewApply() { when(records.latest(2)).thenReturn(record("VERIFIED"));assertEquals("VERIFIED",service.apply().status());verify(records,never()).insert(any()); }
    @Test void changedSchoolNeverDisplaysVerifiedOrQueriesOldRequest() { var r=record("VERIFIED");r.setSchool("旧大学");when(records.latest(2)).thenReturn(r);assertEquals("INVALIDATED",service.mine().status());service.refresh();verify(provider,never()).check(any()); }
    @Test void restoredOldFieldsDoNotReactivateInvalidatedRecord() { when(records.latest(2)).thenReturn(record("INVALIDATED"));assertEquals("INVALIDATED",service.mine().status()); }
    @Test void expiredVerifiedRecordIsReportedExpired() { var r=record("VERIFIED");r.setExpiresAt(LocalDateTime.now().minusSeconds(1));when(records.latest(2)).thenReturn(r);assertEquals("EXPIRED",service.mine().status()); }
    @Test void resultDoesNotExposeFullStudentNumberOrEvidenceReference() { var r=record("VERIFIED");r.setEvidenceReference("secret-provider-reference");when(records.latest(2)).thenReturn(r);var result=service.mine();assertEquals("****0001",result.maskedStudentNumber());assertFalse(result.toString().contains("20260001"));assertFalse(result.toString().contains("secret-provider-reference")); }
    @Test void pendingResultRetainsSameRequestForRetry() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.PENDING,null,null));assertEquals("PENDING",service.refresh().status());verify(provider).check(argThat(r->r.requestId().equals("request-test")));verify(records,never()).complete(any()); }
    @Test void providerFailureRetainsPendingRecord() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenThrow(new RuntimeException("private response"));var e=error(()->service.refresh(),502);assertFalse(e.getMessage().contains("private response"));verify(records,never()).complete(any()); }
    @Test void invalidProviderResultCannotCertify() { when(records.latest(2)).thenReturn(record("PENDING"));error(()->service.refresh(),502);when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.VERIFIED,null,null));error(()->service.refresh(),502);verify(records,never()).complete(any()); }
    @Test void expiredProviderEvidenceCannotCertify() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.VERIFIED,"ref",LocalDateTime.now().minusDays(1)));error(()->service.refresh(),502);verify(records,never()).complete(any()); }
    @Test void onlyServerProviderCanCompleteVerificationWithExpiry() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.VERIFIED,"ref",LocalDateTime.now().plusDays(10)));assertEquals("VERIFIED",service.refresh().status());verify(records).complete(argThat(r->r.getStatus().equals("VERIFIED")&&r.getCheckedAt()!=null&&r.getEvidenceReference().equals("ref"))); }
    @Test void rejectionCanBeRecordedWithoutClaimingVerified() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.REJECTED,"ref",null));assertEquals("REJECTED",service.refresh().status()); }
    @Test void changedProviderCannotHandleOldApplication() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.key()).thenReturn("another");error(()->service.refresh(),409);verify(provider,never()).check(any()); }
    @Test void conflictingResultIsNotReportedAsSuccessful() { when(records.latest(2)).thenReturn(record("PENDING"));when(provider.check(any())).thenReturn(new CampusVerificationProvider.Result(CampusVerificationProvider.Outcome.REJECTED,"ref",null));when(records.complete(any())).thenReturn(0);error(()->service.refresh(),409); }
    @Test void defaultAdapterIsUnavailableAndNeverSimulatesSuccess() { var adapter=new CampusVerificationConfiguration().unavailableCampusProvider();assertFalse(adapter.available());assertThrows(BusinessException.class,()->adapter.check(new CampusVerificationProvider.Request("test","学校","2026"))); }
}
