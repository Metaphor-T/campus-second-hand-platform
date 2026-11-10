package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.UserProfileMapper;
import com.example.mapper.CampusVerificationMapper;
import com.example.pojo.dto.UserProfilePasswordDTO;
import com.example.pojo.dto.UserProfileUpdateDTO;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.UserProfileVO;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final BaseUserService users;
    private final UserProfileMapper profiles;
    private final ProductImageStorageService images;
    private final CampusVerificationMapper verificationRecords;

    private BaseUser requireIdentity(String role) {
        Integer id=UserContext.getUserId();
        if(id==null || !role.equals(UserContext.getRole())) throw new BusinessException("请使用对应身份登录",403);
        BaseUser user=users.getById(id);
        boolean identityExists="USER".equals(role)?profiles.hasUserRole(id):profiles.hasAdminRole(id);
        if(user==null || !Objects.equals(user.getLoginStatus(),1) || !identityExists)
            throw new BusinessException("账号已失效或被禁用，请重新登录",403);
        return user;
    }
    public UserProfileVO mine() { return getProfile(requireIdentity("USER").getId()); }
    public UserProfileVO forAdmin(Integer id) { requireIdentity("ADMIN"); return getProfile(id); }
    private UserProfileVO getProfile(Integer id) {
        UserProfileVO result=profiles.selectProfile(id);
        if(result==null) throw new BusinessException("用户资料不存在",404);
        return result;
    }
    private String optional(String value) { return value==null || value.isBlank()?null:value.strip(); }

    @Transactional(rollbackFor=Exception.class)
    public UserProfileVO save(UserProfileUpdateDTO dto,MultipartFile avatar) {
        Integer id=UserContext.getUserId();
        if(id==null || !"USER".equals(UserContext.getRole())) throw new BusinessException("请使用普通用户身份登录",403);
        if(profiles.lockActiveUser(id)==null) throw new BusinessException("账号状态已变化，请重新登录",403);
        requireIdentity("USER");
        dto.setUsername(dto.getUsername().strip()); dto.setPhone(optional(dto.getPhone()));
        dto.setSchool(optional(dto.getSchool()));dto.setCollege(optional(dto.getCollege()));dto.setStudentNumber(optional(dto.getStudentNumber()));
        String uploaded=avatar==null?null:images.storeAvatar(id,avatar);
        if(uploaded!=null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status!=STATUS_COMMITTED) images.discardAvatar(id,uploaded);
                }
            });
        }
        try {
            if(profiles.updateBasic(id,dto,uploaded)!=1) throw new BusinessException("账号状态已变化，请重新登录",409);
            profiles.upsertCampus(id,dto);
            verificationRecords.invalidateChanged(id,dto.getSchool(),dto.getStudentNumber());
            return getProfile(id);
        } catch(RuntimeException e) {
            if(uploaded!=null) images.discardAvatar(id,uploaded);
            throw e;
        }
    }
    @Transactional(rollbackFor=Exception.class)
    public void changePassword(UserProfilePasswordDTO dto) {
        BaseUser user=requireIdentity("USER");
        if(!Objects.equals(dto.getOldPassword(),user.getPassword())) throw new BusinessException("原密码不正确",400);
        if(!Objects.equals(dto.getNewPassword(),dto.getConfirmPassword())) throw new BusinessException("两次新密码不一致",400);
        if(Objects.equals(dto.getOldPassword(),dto.getNewPassword())) throw new BusinessException("新密码不能与原密码相同",400);
        if(profiles.changePassword(user.getId(),dto.getOldPassword(),dto.getNewPassword())!=1)
            throw new BusinessException("账号或密码已变化，请重新登录",409);
    }
}
