package com.example.profile;

import com.example.exception.BusinessException;
import com.example.mapper.UserProfileMapper;
import com.example.pojo.dto.*;
import com.example.pojo.entity.BaseUser;
import com.example.service.*;
import com.example.utils.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminProfileSecurityTest {
    SessionIdentityService identity;UserProfileMapper mapper;BaseUserService users;ProductImageStorageService images;AdminProfileService service;
    @BeforeEach void setup() {
        UserContext.setUser(1,"ADMIN");identity=mock(SessionIdentityService.class);mapper=mock(UserProfileMapper.class);users=mock(BaseUserService.class);images=mock(ProductImageStorageService.class);
        service=new AdminProfileService(identity,mapper,users,images);when(identity.require("ADMIN")).thenReturn(1);when(mapper.lockActiveUser(1)).thenReturn(1);when(mapper.updateBasic(eq(1),any(),any())).thenReturn(1);
        when(users.getById(1)).thenReturn(BaseUser.builder().id(1).account("admin").password("Original123").username("管理员").loginStatus(1).build());
    }
    @AfterEach void clear() { UserContext.clear();if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization(); }
    AdminProfileUpdateDTO data() { var d=new AdminProfileUpdateDTO();d.setUsername("  管理员  ");d.setPhone("");d.setGender(1);return d; }
    UserProfilePasswordDTO password(String old,String next,String confirm) { var d=new UserProfilePasswordDTO();d.setOldPassword(old);d.setNewPassword(next);d.setConfirmPassword(confirm);return d; }
    @Test void selfUpdateUsesAuthenticatedIdAndAllowsOnlyProfileFields() { var result=service.save(data(),null);assertEquals(1,result.getId());verify(mapper).updateBasic(eq(1),argThat(d->d.getUsername().equals("管理员")&&d.getPhone()==null&&d.getSchool()==null),isNull());verify(users,never()).updateById(any(BaseUser.class)); }
    @Test void ordinaryUserCannotInvokeAdminProfileService() { UserContext.setUser(2,"USER");assertEquals(403,assertThrows(BusinessException.class,()->service.save(data(),null)).getCode());verifyNoInteractions(images,users); }
    @Test void inactiveIdentityCannotWrite() { when(mapper.lockActiveUser(1)).thenReturn(null);assertThrows(BusinessException.class,()->service.save(data(),null));verifyNoInteractions(images); }
    @Test void avatarOwnerComesFromTokenAndOldAvatarIsNeverDeleted() { var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(1,file)).thenReturn("/files/avatars/1/new.png");service.save(data(),file);verify(mapper).updateBasic(eq(1),any(),eq("/files/avatars/1/new.png"));verify(images,never()).discardAvatar(any(),any()); }
    @Test void failedAvatarSaveCleansNewFile() { var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(1,file)).thenReturn("/files/avatars/1/new.png");when(mapper.updateBasic(eq(1),any(),any())).thenReturn(0);assertThrows(BusinessException.class,()->service.save(data(),file));verify(images).discardAvatar(1,"/files/avatars/1/new.png"); }
    @Test void transactionRollbackAfterReturnAlsoCleansNewFile() { TransactionSynchronizationManager.initSynchronization();var file=new MockMultipartFile("avatar","a.png","image/png",new byte[]{1});when(images.storeAvatar(1,file)).thenReturn("/files/avatars/1/new.png");service.save(data(),file);for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);verify(images).discardAvatar(1,"/files/avatars/1/new.png"); }
    @Test void oldPasswordIsRequired() { assertThrows(BusinessException.class,()->service.password(password("wrong","NewPass123","NewPass123")));verify(mapper,never()).changePassword(any(),any(),any()); }
    @Test void confirmationAndDifferentPasswordAreRequired() { assertThrows(BusinessException.class,()->service.password(password("Original123","NewPass123","other")));assertThrows(BusinessException.class,()->service.password(password("Original123","Original123","Original123")));verify(mapper,never()).changePassword(any(),any(),any()); }
    @Test void correctPasswordChangesOnlyCurrentAdmin() { when(mapper.changePassword(1,"Original123","NewPass123")).thenReturn(1);service.password(password("Original123","NewPass123","NewPass123"));verify(mapper).changePassword(1,"Original123","NewPass123"); }
    @Test void concurrentPasswordChangeFailsWithoutOverwriting() { assertEquals(409,assertThrows(BusinessException.class,()->service.password(password("Original123","NewPass123","NewPass123"))).getCode()); }
}
