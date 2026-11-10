package com.example.profile;

import com.example.exception.BusinessException;
import com.example.mapper.UserProfileMapper;
import com.example.mapper.CampusVerificationMapper;
import com.example.pojo.dto.*;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.UserProfileVO;
import com.example.service.*;
import com.example.utils.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    BaseUserService users;UserProfileMapper mapper;ProductImageStorageService images;UserProfileService service;CampusVerificationMapper verification;
    static UserProfileVO profile() { return new UserProfileVO(2,"student","同学","/files/original.png",1,null,LocalDateTime.of(2026,6,21,12,0),null,null,null,"USER"); }
    UserProfileUpdateDTO data() { var dto=new UserProfileUpdateDTO();dto.setUsername("  同学  ");dto.setPhone("");dto.setSchool("  校园大学  ");dto.setCollege("");dto.setStudentNumber("T_2026");return dto; }
    UserProfilePasswordDTO password(String old,String next,String confirm) { var dto=new UserProfilePasswordDTO();dto.setOldPassword(old);dto.setNewPassword(next);dto.setConfirmPassword(confirm);return dto; }
    @BeforeEach void setup() {
        users=mock(BaseUserService.class);mapper=mock(UserProfileMapper.class);images=mock(ProductImageStorageService.class);
        verification=mock(CampusVerificationMapper.class);
        service=new UserProfileService(users,mapper,images,verification);UserContext.setUser(2,"USER");
        when(mapper.lockActiveUser(2)).thenReturn(2);
        when(users.getById(2)).thenReturn(BaseUser.builder().id(2).loginStatus(1).password("OldPass123").build());
        when(mapper.hasUserRole(2)).thenReturn(true);when(mapper.selectProfile(2)).thenReturn(profile());when(mapper.updateBasic(eq(2),any(),any())).thenReturn(1);
    }
    @AfterEach void clear() { UserContext.clear();if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization(); }
    @Test void ownProfileIncludesIdentityAndNoPassword() { assertEquals(profile(),service.mine()); }
    @Test void noIdentityCannotReadOrSave() { UserContext.clear();assertThrows(BusinessException.class,()->service.mine());verifyNoInteractions(images); }
    @Test void disabledUserCannotRead() { when(users.getById(2)).thenReturn(BaseUser.builder().id(2).loginStatus(2).build());assertEquals(403,assertThrows(BusinessException.class,()->service.mine()).getCode()); }
    @Test void removedRoleCannotRead() { when(mapper.hasUserRole(2)).thenReturn(false);assertThrows(BusinessException.class,()->service.mine()); }
    @Test void normalUserCannotReadAdminProfileRoute() { assertEquals(403,assertThrows(BusinessException.class,()->service.forAdmin(3)).getCode());verify(mapper,never()).selectProfile(3); }
    @Test void administratorCanReadOrdinaryProfile() { UserContext.setUser(1,"ADMIN");when(users.getById(1)).thenReturn(BaseUser.builder().id(1).loginStatus(1).build());when(mapper.hasAdminRole(1)).thenReturn(true);assertEquals(profile(),service.forAdmin(2)); }
    @Test void saveNormalizesOptionalFieldsAndUsesCurrentIdentity() {
        var dto=data();service.save(dto,null);
        verify(mapper).updateBasic(eq(2),argThat(d->d.getUsername().equals("同学")&&d.getPhone()==null&&d.getSchool().equals("校园大学")&&d.getCollege()==null),isNull());
        verify(mapper).upsertCampus(2,dto);verifyNoInteractions(images);
        var sequence=inOrder(mapper,verification);sequence.verify(mapper).lockActiveUser(2);sequence.verify(mapper).upsertCampus(2,dto);sequence.verify(verification).invalidateChanged(2,"校园大学","T_2026");
    }
    @Test void successfulAvatarSaveDoesNotDeleteOriginalFile() {
        var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(2,file)).thenReturn("/files/avatars/2/new.png");
        service.save(data(),file);verify(mapper).updateBasic(eq(2),any(),eq("/files/avatars/2/new.png"));verify(images,never()).discardAvatar(any(),any());
    }
    @Test void failedDatabaseSaveRemovesOnlyNewAvatar() {
        var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(2,file)).thenReturn("/files/avatars/2/new.png");doThrow(new RuntimeException("db")).when(mapper).upsertCampus(any(),any());
        assertThrows(RuntimeException.class,()->service.save(data(),file));verify(images).discardAvatar(2,"/files/avatars/2/new.png");
    }
    @Test void rollbackAfterMethodReturnStillCleansNewAvatar() {
        TransactionSynchronizationManager.initSynchronization();var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(2,file)).thenReturn("/files/avatars/2/new.png");
        service.save(data(),file);for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(images).discardAvatar(2,"/files/avatars/2/new.png");
    }
    @Test void correctPasswordChangesOnlyCurrentUser() { when(mapper.changePassword(2,"OldPass123","NewPass123")).thenReturn(1);service.changePassword(password("OldPass123","NewPass123","NewPass123"));verify(mapper).changePassword(2,"OldPass123","NewPass123"); }
    @Test void wrongOldPasswordCannotChange() { assertThrows(BusinessException.class,()->service.changePassword(password("bad","NewPass123","NewPass123")));verify(mapper,never()).changePassword(any(),any(),any()); }
    @Test void confirmationMustMatch() { assertThrows(BusinessException.class,()->service.changePassword(password("OldPass123","NewPass123","different")));verify(mapper,never()).changePassword(any(),any(),any()); }
    @Test void identicalPasswordIsRejected() { assertThrows(BusinessException.class,()->service.changePassword(password("OldPass123","OldPass123","OldPass123"))); }
    @Test void concurrentPasswordChangeIsRejected() { assertEquals(409,assertThrows(BusinessException.class,()->service.changePassword(password("OldPass123","NewPass123","NewPass123"))).getCode()); }
}
