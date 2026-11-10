package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.SessionIdentityMapper;
import com.example.pojo.dto.*;
import com.example.pojo.vo.BaseUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AccountManagementService {
    private final SessionIdentityService identity;
    private final SessionIdentityMapper identities;
    private final BaseUserService accounts;
    private final UserService users;
    private final AdminService admins;

    private void target(Integer id,boolean admin) {
        var record=id==null?null:identities.find(id);
        if(record==null || (admin?!record.adminRole():!record.userRole()||record.adminRole()))
            throw new BusinessException("目标账号不属于当前管理范围",404);
    }
    @Transactional(rollbackFor=Exception.class)
    public void create(AccountCreateDTO dto,boolean admin) {
        identity.require("ADMIN");
        var account=dto.entity();
        if(accounts.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.pojo.entity.BaseUser>()
            .eq(com.example.pojo.entity.BaseUser::getAccount,account.getAccount()))>0)
            throw new BusinessException("登录账号已存在",409);
        if(admin)admins.add(account);else users.add(account);
    }
    @Transactional(rollbackFor=Exception.class)
    public void update(AccountUpdateDTO dto,boolean admin) {
        if(admin)identities.lockAdministrators();
        identity.require("ADMIN");target(dto.getId(),admin);
        if(admin && Integer.valueOf(2).equals(dto.getLoginStatus())) protectLastActive(dto.getId());
        if(admin)admins.updateAdminInfo(dto.entity());else users.updateAdminInfo(dto.entity());
    }
    @Transactional(rollbackFor=Exception.class)
    public void delete(Integer id,boolean admin) {
        if(admin)identities.lockAdministrators();
        identity.require("ADMIN");target(id,admin);
        if(admin) { protectLastActive(id);admins.deleteById(id); } else users.deleteById(id);
    }
    public BaseUserVO detail(Integer id,boolean admin) {
        identity.require("ADMIN");target(id,admin);return admin?admins.detail(id):users.detail(id);
    }
    private void protectLastActive(Integer id) {
        var record=identities.find(id);
        if(Integer.valueOf(1).equals(record.loginStatus()) && identities.activeAdministratorCount()<=1)
            throw new BusinessException("请至少保留一名可登录的管理员",409);
    }
}
