package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Admin;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.entity.User;
import com.example.pojo.vo.BaseUserVO;
import com.example.pojo.vo.UserLoginResponseVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 用户业务逻辑接口
 */
public interface UserService extends IService<User> {

    UserLoginResponseVO login(UserLoginDTO userLoginDTO);

    void register(UserRegisterDTO userRegisterDTO);

    BaseUserVO tokenAuth();

    void updatePassword(Integer id, UserUpdatePasswordDTO userUpdatePasswordDTO);

    void updateInfo(BaseUserUpdateDTO baseUserUpdateDTO);

    void uploadAvatar(BaseUserUpdateDTO baseUserUpdateDTO);

    List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO);

    Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO);

    void add(BaseUser baseUser);

    void updateAdminInfo(BaseUser baseUser);

    void deleteById(Integer id);

    BaseUserVO detail(Integer baseUserId);

}