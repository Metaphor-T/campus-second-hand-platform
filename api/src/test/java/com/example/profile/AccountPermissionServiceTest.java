package com.example.profile;

import com.example.exception.BusinessException;
import com.example.mapper.SessionIdentityMapper;
import com.example.pojo.dto.*;
import com.example.service.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountPermissionServiceTest {
    SessionIdentityService identity;SessionIdentityMapper mapper;BaseUserService accounts;UserService users;AdminService admins;AccountManagementService service;
    @BeforeEach void setup() {
        identity=mock(SessionIdentityService.class);mapper=mock(SessionIdentityMapper.class);accounts=mock(BaseUserService.class);users=mock(UserService.class);admins=mock(AdminService.class);
        service=new AccountManagementService(identity,mapper,accounts,users,admins);
        when(mapper.find(2)).thenReturn(new SessionIdentityMapper.Identity(2,1,true,false,"x"));when(mapper.find(1)).thenReturn(new SessionIdentityMapper.Identity(1,1,false,true,"x"));when(mapper.activeAdministratorCount()).thenReturn(1);
    }
    AccountUpdateDTO change(int id,int status) { var d=new AccountUpdateDTO();d.setId(id);d.setUsername("同学");d.setLoginStatus(status);return d; }
    @Test void userManagementCannotDeleteAdminAndBypassLastAdminGuard() { assertEquals(404,assertThrows(BusinessException.class,()->service.delete(1,false)).getCode());verifyNoInteractions(users,admins); }
    @Test void userManagementCannotEditAdmin() { assertThrows(BusinessException.class,()->service.update(change(1,2),false));verifyNoInteractions(users,admins); }
    @Test void adminManagementCannotDeleteOrdinaryUser() { assertThrows(BusinessException.class,()->service.delete(2,true));verifyNoInteractions(users,admins); }
    @Test void nonexistentTargetIsRejected() { assertThrows(BusinessException.class,()->service.detail(999,false));verifyNoInteractions(users,admins); }
    @Test void lastActiveAdminCannotBeDisabled() { assertEquals(409,assertThrows(BusinessException.class,()->service.update(change(1,2),true)).getCode());verify(admins,never()).updateAdminInfo(any()); }
    @Test void lastActiveAdminCannotBeDeleted() { assertEquals(409,assertThrows(BusinessException.class,()->service.delete(1,true)).getCode());verify(admins,never()).deleteById(any()); }
    @Test void administratorRowsLockedBeforeCheckingCurrentIdentity() { assertThrows(BusinessException.class,()->service.delete(1,true));var order=inOrder(mapper,identity);order.verify(mapper).lockAdministrators();order.verify(identity).require("ADMIN"); }
    @Test void secondActiveAdminMayBeDisabled() { when(mapper.activeAdministratorCount()).thenReturn(2);service.update(change(1,2),true);verify(admins).updateAdminInfo(argThat(d->d.getId()==1&&d.getLoginStatus()==2&&d.getPassword()==null&&d.getAccount()==null&&d.getAvatar()==null&&d.getCreateTime()==null)); }
    @Test void ordinaryUserCanBeManagedThroughCorrectRoute() { service.update(change(2,2),false);verify(users).updateAdminInfo(argThat(d->d.getId()==2&&d.getLoginStatus()==2));service.delete(2,false);verify(users).deleteById(2); }
    @Test void serviceRejectsOperationWhenCurrentAdminIsDisabled() { when(identity.require("ADMIN")).thenThrow(new BusinessException("disabled",401));assertThrows(BusinessException.class,()->service.update(change(2,2),false));verifyNoInteractions(users,admins); }
    @Test void newAdminIsCreatedOnlyAfterCurrentAdminValidation() { var dto=new AccountCreateDTO();dto.setAccount("new_admin");dto.setPassword("AdminTest123");dto.setUsername("管理员");service.create(dto,true);var order=inOrder(identity,admins);order.verify(identity).require("ADMIN");order.verify(admins).add(argThat(u->u.getId()==null&&u.getAccount().equals("new_admin")&&u.getAvatar()==null)); }
    @Test void duplicateAccountIsRejected() { when(accounts.count(any())).thenReturn(1L);var dto=new AccountCreateDTO();dto.setAccount("admin");dto.setUsername("管理员");dto.setPassword("AdminTest123");assertEquals(409,assertThrows(BusinessException.class,()->service.create(dto,true)).getCode());verifyNoInteractions(admins); }
}
