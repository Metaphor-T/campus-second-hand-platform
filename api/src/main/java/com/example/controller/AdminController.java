package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.*;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.BaseUserVO;
import com.example.pojo.vo.UserLoginResponseVO;
import com.example.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员信息控制器
 */
@RestController
@RequestMapping(value = "/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 管理员新增
     *
     * @return Result<Void>
     */
    @Auth({RoleEnum.ADMIN})
    @PostMapping(value = "/add")
    public Result<Void> add(@Valid @RequestBody BaseUser baseUser) {
        adminService.add(baseUser);
        return Result.success();
    }

    /**
     * 管理员修改
     *
     * @return Result<Void>
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/update")
    public Result<Void> update(@Valid @RequestBody BaseUser baseUser) {
        adminService.updateAdminInfo(baseUser);
        return Result.success();
    }

    /**
     * 删除管理员
     *
     * @param baseUserId 基础用户信息ID
     * @return Result<Void>
     */
    @Auth({RoleEnum.ADMIN})
    @DeleteMapping(value = "/{baseUserId}")
    public Result<Void> deleteById(@PathVariable Integer baseUserId) {
        adminService.deleteById(baseUserId);
        return Result.success();
    }

    /**
     * 通过主键ID查询管理员详情
     *
     * @param baseUserId 基础用户信息ID
     * @return Result<BaseUserVO>
     */
    @Auth({RoleEnum.ADMIN})
    @DeleteMapping(value = "/{baseUserId}/detail")
    public Result<BaseUserVO> detail(@PathVariable Integer baseUserId) {
        BaseUserVO baseUserVO = adminService.detail(baseUserId);
        return Result.success(baseUserVO);
    }

    /**
     * 登录
     *
     * @return Result<UserLoginResponseVO>
     */
    @PostMapping(value = "/login")
    public Result<UserLoginResponseVO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        UserLoginResponseVO userLoginResponseVO = adminService.login(userLoginDTO);
        return Result.success(userLoginResponseVO);
    }

    /**
     * token认证
     *
     * @return Result<BaseUserVO>
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/auth")
    public Result<BaseUserVO> tokenAuth() {
        BaseUserVO baseUserVO = adminService.tokenAuth();
        return Result.success(baseUserVO);
    }

    /**
     * 注册
     *
     * @return String
     */
    @PostMapping(value = "/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        adminService.register(userRegisterDTO);
        return Result.success();
    }

    /**
     * 修改个人信息
     *
     * @return String
     */
    @PutMapping
    public Result<Void> updateInfo(@Valid @RequestBody BaseUserUpdateDTO baseUserUpdateDTO) {
        adminService.updateInfo(baseUserUpdateDTO);
        return Result.success();
    }

    /**
     * 上传头像
     *
     * @return Result<Void>
     */
    @PutMapping(value = "/uploadAvatar")
    public Result<Void> uploadAvatar(@Valid @RequestBody BaseUserUpdateDTO baseUserUpdateDTO) {
        adminService.uploadAvatar(baseUserUpdateDTO);
        return Result.success();
    }

    /**
     * 修改密码
     *
     * @return Result<Void>
     */
    @PutMapping(value = "/{id}/updatePassword")
    public Result<Void> updatePassword(@PathVariable Integer id,
                                        @Valid @RequestBody UserUpdatePasswordDTO userUpdatePasswordDTO) {
        adminService.updatePassword(id, userUpdatePasswordDTO);
        return Result.success();
    }

    /**
     * 列表查询
     *
     * @return Result<List < BaseUserVO>>
     */
    @Pager
    @Auth({RoleEnum.ADMIN})
    @PostMapping(value = "/listPage")
    public Result<List<BaseUserVO>> listPage(@RequestBody BaseUserQueryDTO baseUserQueryDTO) {
        List<BaseUserVO> baseUserVOS = adminService.listPage(baseUserQueryDTO);
        Integer count = adminService.listPageCount(baseUserQueryDTO);
        return Result.success(baseUserVOS, count);
    }


}
