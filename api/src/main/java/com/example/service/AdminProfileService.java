package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.UserProfileMapper;
import com.example.pojo.dto.*;
import com.example.pojo.vo.BaseUserVO;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class AdminProfileService {
    private final SessionIdentityService identity;
    private final UserProfileMapper profiles;
    private final BaseUserService users;
    private final ProductImageStorageService images;
    private Integer lockSelf() {
        Integer id=UserContext.getUserId();
        if(!"ADMIN".equals(UserContext.getRole()) || id==null) throw new BusinessException("请使用管理员身份登录",403);
        if(profiles.lockActiveUser(id)==null) throw new BusinessException("账号已失效，请重新登录",401);
        return identity.require("ADMIN");
    }
    @Transactional(rollbackFor=Exception.class)
    public BaseUserVO save(AdminProfileUpdateDTO data,MultipartFile file) {
        Integer id=lockSelf();
        var dto=new UserProfileUpdateDTO();dto.setUsername(data.getUsername().strip());dto.setGender(data.getGender());
        dto.setPhone(data.getPhone()==null || data.getPhone().isBlank()?null:data.getPhone().strip());
        String avatar=file==null?null:images.storeAvatar(id,file);
        if(avatar!=null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) images.discardAvatar(id,avatar); }
            });
        }
        try {
            if(profiles.updateBasic(id,dto,avatar)!=1) throw new BusinessException("账号状态已变化，请重新登录",409);
            var saved=users.getById(id);
            return BaseUserVO.builder().id(id).account(saved.getAccount()).username(saved.getUsername()).avatar(saved.getAvatar())
                .phone(saved.getPhone()).gender(saved.getGender()).loginStatus(saved.getLoginStatus()).createTime(saved.getCreateTime()).build();
        } catch(RuntimeException e) { if(avatar!=null)images.discardAvatar(id,avatar);throw e; }
    }
    @Transactional(rollbackFor=Exception.class)
    public void password(UserProfilePasswordDTO dto) {
        Integer id=lockSelf();var current=users.getById(id);
        if(!Objects.equals(current.getPassword(),dto.getOldPassword())) throw new BusinessException("原密码不正确",400);
        if(!Objects.equals(dto.getNewPassword(),dto.getConfirmPassword())) throw new BusinessException("两次新密码不一致",400);
        if(Objects.equals(dto.getOldPassword(),dto.getNewPassword())) throw new BusinessException("新密码不能与原密码相同",400);
        if(profiles.changePassword(id,dto.getOldPassword(),dto.getNewPassword())!=1) throw new BusinessException("账号或密码已变化，请重新登录",409);
    }
}
