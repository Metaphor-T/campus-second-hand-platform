package com.example.profile;

import com.example.controller.*;
import com.example.exception.UserProfileExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.mapper.SessionIdentityMapper;
import com.example.service.*;
import com.example.utils.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IdentityPermissionHttpTest {
    SessionIdentityMapper mapper;JwtUtil jwt;MockMvc mvc;AccountManagementService management;UserService users;AdminService admins;AdminProfileService adminProfiles;
    String token(int id,String role) { return jwt.generateToken(id,role,"Original123"); }
    @BeforeEach void setup() {
        mapper=mock(SessionIdentityMapper.class);jwt=IdentitySecurityTest.jwt((byte)31);management=mock(AccountManagementService.class);users=mock(UserService.class);admins=mock(AdminService.class);adminProfiles=mock(AdminProfileService.class);
        when(mapper.find(2)).thenReturn(new SessionIdentityMapper.Identity(2,1,true,false,JwtUtil.passwordDigest("Original123")));
        when(mapper.find(1)).thenReturn(new SessionIdentityMapper.Identity(1,1,false,true,JwtUtil.passwordDigest("Original123")));
        mvc=MockMvcBuilders.standaloneSetup(new UserController(users,management),new AdminController(admins,management),new AdminProfileController(adminProfiles))
            .setControllerAdvice(new UserProfileExceptionHandler(),new com.example.exception.GlobalExceptionHandler()).addInterceptors(new JwtInterceptor(jwt,new SessionIdentityService(mapper,jwt))).build();
    }
    @ParameterizedTest @ValueSource(strings={"POST /user/add","PUT /user/update","DELETE /user/3","DELETE /user/3/detail","POST /user/listPage","POST /admin/add","PUT /admin/update","DELETE /admin/1","DELETE /admin/1/detail","POST /admin/listPage","PUT /admin/profile","PUT /admin/profile/password"})
    void anonymousCannotReachIdentityOperations(String spec) throws Exception { String[] p=spec.split(" ");mvc.perform(request(HttpMethod.valueOf(p[0]),p[1]).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());verifyNoInteractions(management,users,admins,adminProfiles); }
    @ParameterizedTest @ValueSource(strings={"POST /user/add","PUT /user/update","DELETE /user/3","DELETE /user/3/detail","POST /user/listPage","POST /admin/add","PUT /admin/update","DELETE /admin/1","DELETE /admin/1/detail","POST /admin/listPage","PUT /admin/profile","PUT /admin/profile/password"})
    void ordinaryUserCannotReachAdminOperations(String spec) throws Exception { String[] p=spec.split(" ");mvc.perform(request(HttpMethod.valueOf(p[0]),p[1]).contentType(MediaType.APPLICATION_JSON).header("Authorization","Bearer "+token(2,"USER"))).andExpect(status().isForbidden());verifyNoInteractions(management,users,admins,adminProfiles); }
    @Test void removedAdminRegistrationRouteCannotBePostedForEveryRole() throws Exception { mvc.perform(post("/admin/register")).andExpect(status().isMethodNotAllowed());mvc.perform(post("/admin/register").header("Authorization","Bearer "+token(1,"ADMIN"))).andExpect(status().isMethodNotAllowed());verifyNoInteractions(management,admins); }
    @Test void adminClaimWithOrdinaryDatabaseRoleIs401() throws Exception { mvc.perform(post("/admin/add").header("Authorization","Bearer "+token(2,"ADMIN"))).andExpect(status().isUnauthorized());verifyNoInteractions(management); }
    @Test void bannedTokenIsRejectedBeforeAnyManagementAction() throws Exception { when(mapper.find(1)).thenReturn(new SessionIdentityMapper.Identity(1,2,false,true,JwtUtil.passwordDigest("Original123")));mvc.perform(delete("/user/2").header("Authorization","Bearer "+token(1,"ADMIN"))).andExpect(status().isUnauthorized());verifyNoInteractions(management); }
    @Test void deletedAdminRoleIsRejected() throws Exception { when(mapper.find(1)).thenReturn(new SessionIdentityMapper.Identity(1,1,false,false,JwtUtil.passwordDigest("Original123")));mvc.perform(delete("/user/2").header("Authorization","Bearer "+token(1,"ADMIN"))).andExpect(status().isUnauthorized());verifyNoInteractions(management); }
    @Test void stalePasswordTokenIsRejected() throws Exception { when(mapper.find(1)).thenReturn(new SessionIdentityMapper.Identity(1,1,false,true,JwtUtil.passwordDigest("Changed123")));mvc.perform(delete("/user/2").header("Authorization","Bearer "+token(1,"ADMIN"))).andExpect(status().isUnauthorized());verifyNoInteractions(management); }
    @Test void forgedManagementFieldsCannotChangeAccountPasswordOrAvatar() throws Exception { mvc.perform(put("/user/update").header("Authorization","Bearer "+token(1,"ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"id\":2,\"username\":\"同学\",\"account\":\"admin\",\"password\":\"forged\",\"avatar\":\"/files/other.png\",\"role\":\"ADMIN\"}")).andExpect(status().isOk());verify(management).update(argThat(d->d.entity().getAccount()==null&&d.entity().getPassword()==null&&d.entity().getAvatar()==null&&d.getId()==2),eq(false)); }
    @Test void forgedSelfTargetIsNotPartOfAdminProfileCommand() throws Exception { mvc.perform(put("/admin/profile").header("Authorization","Bearer "+token(1,"ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"id\":2,\"username\":\"管理员\",\"loginStatus\":2,\"role\":\"USER\"}")).andExpect(status().isOk());verify(adminProfiles).save(argThat(d->d.getUsername().equals("管理员")),isNull()); }
    @Test void oldAdminPasswordWithoutProofIsUnavailable() throws Exception { mvc.perform(put("/admin/1/updatePassword").header("Authorization","Bearer "+token(1,"ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"NewPass123\"}")).andExpect(status().isGone());verifyNoInteractions(adminProfiles); }
    @Test void oldOrdinarySelfWritersAreUnavailable() throws Exception { mvc.perform(put("/user").header("Authorization","Bearer "+token(2,"USER")).contentType(MediaType.APPLICATION_JSON).content("{\"id\":2,\"username\":\"同学\"}")).andExpect(status().isGone());verifyNoInteractions(users); }
    @ParameterizedTest @ValueSource(strings={"/user","/user/uploadAvatar","/user/3/updatePassword"})
    void legacyPersonalEndpointsCannotModifyAnotherUser(String path) throws Exception { mvc.perform(put(path).header("Authorization","Bearer "+token(2,"USER")).contentType(MediaType.APPLICATION_JSON).content("{\"id\":3,\"username\":\"其他人\",\"oldPassword\":\"Original123\",\"newPassword\":\"NewPass123\",\"confirmPassword\":\"NewPass123\"}")).andExpect(status().isForbidden());verifyNoInteractions(users); }
    @Test void newAdminPasswordRequiresOldPassword() throws Exception { mvc.perform(put("/admin/profile/password").header("Authorization","Bearer "+token(1,"ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"NewPass123\",\"confirmPassword\":\"NewPass123\"}")).andExpect(status().isBadRequest());verifyNoInteractions(adminProfiles); }
}
