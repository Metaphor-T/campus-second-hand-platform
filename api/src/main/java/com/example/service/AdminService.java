package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Admin;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.BaseUserVO;
import com.example.pojo.vo.UserLoginResponseVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 管理员业务逻辑接口
 */
public interface AdminService extends IService<Admin> {

    UserLoginResponseVO login(UserLoginDTO userLoginDTO);

    void register(UserRegisterDTO userRegisterDTO);

    BaseUserVO tokenAuth();

    void updatePassword(Integer id, UserUpdatePasswordDTO userUpdatePasswordDTO);

    void updateInfo( BaseUserUpdateDTO baseUserUpdateDTO);

    void uploadAvatar(@Valid BaseUserUpdateDTO baseUserUpdateDTO);

    List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO);

    Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO);

    void add(BaseUser baseUser);

    void updateAdminInfo(BaseUser baseUser);

    void deleteById(Integer id);

    BaseUserVO detail(Integer baseUserId);

}
