package com.example.profile;

import com.example.controller.*;
import com.example.exception.*;
import com.example.interceptor.JwtInterceptor;
import com.example.service.*;
import com.example.utils.*;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserProfileHttpTest {
    UserProfileService profiles;UserService users;AdminService admins;JwtUtil jwt;MockMvc mvc;
    @BeforeEach void setup() {
        profiles=mock(UserProfileService.class);users=mock(UserService.class);admins=mock(AdminService.class);jwt=mock(JwtUtil.class);
        mvc=MockMvcBuilders.standaloneSetup(new UserProfileController(profiles),new UserController(users,profiles),new AdminController(admins))
            .setControllerAdvice(new UserProfileExceptionHandler(),new GlobalExceptionHandler()).addInterceptors(new JwtInterceptor(jwt)).build();
    }
    void role(String role) { when(jwt.getClaims("token")).thenReturn(mock(Claims.class));when(jwt.getUserId("token")).thenReturn(role.equals("ADMIN")?"1":"2");when(jwt.getRole("token")).thenReturn(role); }
    @Test void anonymousProfileIs401() throws Exception { mvc.perform(get("/user/profile")).andExpect(status().isUnauthorized());verifyNoInteractions(profiles); }
    @Test void ownEndpointIgnoresTargetIdAndDoesNotExposePassword() throws Exception {
        role("USER");when(profiles.mine()).thenAnswer(inv->{org.junit.jupiter.api.Assertions.assertEquals(2,UserContext.getUserId());return UserProfileServiceTest.profile();});
        mvc.perform(get("/user/profile").param("id","1").header("Authorization","Bearer token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(2)).andExpect(jsonPath("$.data.role").value("USER")).andExpect(jsonPath("$.data.password").doesNotExist());
    }
    @Test void normalUserCannotReadOtherCampusProfile() throws Exception { role("USER");mvc.perform(get("/user/profile/admin/3").header("Authorization","Bearer token")).andExpect(status().isForbidden());verifyNoInteractions(profiles); }
    @Test void administratorCanReadRestrictedCampusProfile() throws Exception { role("ADMIN");when(profiles.forAdmin(2)).thenReturn(UserProfileServiceTest.profile());mvc.perform(get("/user/profile/admin/2").header("Authorization","Bearer token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(2)); }
    @ParameterizedTest @ValueSource(strings={"{\"username\":\" \"}","{\"username\":\"同学\",\"phone\":\"123\"}","{\"username\":\"同学\",\"studentNumber\":\"<bad>\"}","{\"username\":\"同学\",\"gender\":3}","{\"username\":\"同学\",\"school\":\"a\\nb\"}"})
    void invalidProfileRejectedBeforeService(String body) throws Exception { role("USER");mvc.perform(put("/user/profile").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());verifyNoInteractions(profiles); }
    @Test void campusLengthIsBounded() throws Exception { role("USER");mvc.perform(put("/user/profile").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"同学\",\"college\":\""+"院".repeat(101)+"\"}")).andExpect(status().isBadRequest()); }
    @Test void multipartProfileAndAvatarShareOneSaveCall() throws Exception {
        role("USER");var dto=new MockMultipartFile("profile","","application/json","{\"username\":\"同学\",\"school\":\"校园大学\",\"studentNumber\":\"2026001\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(multipart("/user/profile").file(dto).file(new MockMultipartFile("avatar","a.png","image/png",new byte[]{1})).with(r->{r.setMethod("PUT");return r;}).header("Authorization","Bearer token")).andExpect(status().isOk());
        verify(profiles).save(argThat(d->d.getSchool().equals("校园大学")&&d.getStudentNumber().equals("2026001")),any());
    }
    @Test void oldPasswordRequired() throws Exception { role("USER");mvc.perform(put("/user/profile/password").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"NewPass123\",\"confirmPassword\":\"NewPass123\"}")).andExpect(status().isBadRequest());verifyNoInteractions(profiles); }
    @Test void oldPasswordRouteAlsoRequiresProof() throws Exception { role("USER");mvc.perform(put("/user/2/updatePassword").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"NewPass123\"}")).andExpect(jsonPath("$.code").value(400));verifyNoInteractions(profiles); }
    @Test void editingOtherUsersThroughLegacyRouteIsRejected() throws Exception { role("USER");mvc.perform(put("/user").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"id\":3,\"username\":\"不应修改\"}")).andExpect(jsonPath("$.code").value(403));verifyNoInteractions(users); }
    @Test void legacyAvatarRouteCannotSaveArbitraryPath() throws Exception { role("USER");mvc.perform(put("/user/uploadAvatar").header("Authorization","Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"id\":2,\"avatar\":\"/files/another.png\"}")).andExpect(jsonPath("$.code").value(410));verifyNoInteractions(users); }
    @Test void adminRegistrationIsNotPublic() throws Exception { mvc.perform(post("/admin/register").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"someone\",\"username\":\"测试\",\"password\":\"TestPass123\"}")).andExpect(status().isUnauthorized());verifyNoInteractions(admins); }
    @Test void userCannotCreateAdminOrChangeAdminPassword() throws Exception { role("USER");mvc.perform(post("/admin/register").header("Authorization","Bearer token")).andExpect(status().isForbidden());mvc.perform(put("/admin/1/updatePassword").header("Authorization","Bearer token")).andExpect(status().isForbidden());verifyNoInteractions(admins); }
    @Test void userCannotUseManagementMutationRoutes() throws Exception { role("USER");mvc.perform(post("/user/add").header("Authorization","Bearer token")).andExpect(status().isForbidden());mvc.perform(put("/user/update").header("Authorization","Bearer token")).andExpect(status().isForbidden());mvc.perform(delete("/user/3").header("Authorization","Bearer token")).andExpect(status().isForbidden());verifyNoInteractions(users); }
    @Test void userCannotListOtherContactRecords() throws Exception { role("USER");mvc.perform(post("/user/listPage").header("Authorization","Bearer token")).andExpect(status().isForbidden()); }
}
