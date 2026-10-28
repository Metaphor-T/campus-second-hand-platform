package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.UserMapper;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Admin;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.entity.User;
import com.example.pojo.enums.AccountStatusEnum;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.BaseUserVO;
import com.example.pojo.vo.UserLoginResponseVO;
import com.example.service.BaseUserService;
import com.example.service.FileUploadService;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息业务逻辑接口实现类
 **/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BaseUserService baseUserService;
    private final JwtUtil jwtUtil;
    private final FileUploadService fileUploadService;

    /**
     * 登录
     *
     * @param userLoginDTO 登录参数接受类
     */
    @Override
    public UserLoginResponseVO login(UserLoginDTO userLoginDTO) {
        // 通过账号查询账户信息
        BaseUser baseUser = baseUserService.getOne(
                new LambdaQueryWrapper<BaseUser>()
                        .eq(BaseUser::getAccount, userLoginDTO.getAccount())
        );
        if (baseUser == null) {
            throw new BusinessException("用户不存在");
        }
        // 回用户表，查询身份是否正确
        Integer baseUserId = baseUser.getId();
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getBaseUserId, baseUserId));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 密码判断
        if (!Objects.equals(baseUser.getPassword(), userLoginDTO.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 封号判断
        if (AccountStatusEnum.FORBIDDEN_LOGIN.getStatusCode().equals(baseUser.getLoginStatus())) {
            throw new BusinessException("该账号已被封禁，禁止登录！可联系用户咨询！");
        }
        // 生成Token
        String token = jwtUtil.generateToken(baseUser.getId(), RoleEnum.USER.getRole());
        return UserLoginResponseVO
                .builder()
                .token(token)
                .role(RoleEnum.USER.getRole())
                .build();
    }

    /**
     * 注册
     *
     * @param userRegisterDTO 注册参数对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO userRegisterDTO) {
        // 校验账号可用性
        BaseUser baseUser = baseUserService.getOne(
                new LambdaQueryWrapper<BaseUser>()
                        .eq(BaseUser::getAccount, userRegisterDTO.getAccount())
        );
        if (Objects.nonNull(baseUser)) {
            throw new BusinessException("账户不可用");
        }
        // 数据新增（基础信息新增、用户信息新增）
        BaseUser baseUserSaveEntity = BaseUser.builder()
                .account(userRegisterDTO.getAccount())
                .password(userRegisterDTO.getPassword())
                .username(userRegisterDTO.getUsername())
                .loginStatus(AccountStatusEnum.NORMAL.getStatusCode())
                .createTime(LocalDateTime.now())
                .build();
        this.baseUserService.save(baseUserSaveEntity);
        User user = User.builder()
                .baseUserId(baseUserSaveEntity.getId())
                .build();
        this.save(user);
    }

    /**
     * Token认证
     *
     * @return BaseUserVO
     */
    @Override
    public BaseUserVO tokenAuth() {
        UserContext.UserInfo user = UserContext.getUser();
        Integer userId = user.userId();
        BaseUser baseUser = this.baseUserService.getById(userId);
        if (baseUser == null) {
            throw new BusinessException("用户信息查询异常");
        }
        // 封号判断
        if (AccountStatusEnum.FORBIDDEN_LOGIN.getStatusCode().equals(baseUser.getLoginStatus())) {
            throw new BusinessException("该账号已被封禁，禁止使用！可联系用户咨询！");
        }
        return BaseUserVO.builder()
                .id(baseUser.getId())
                .username(baseUser.getUsername())
                .account(baseUser.getAccount())
                .avatar(baseUser.getAvatar())
                .phone(baseUser.getPhone())
                .loginStatus(baseUser.getLoginStatus())
                .build();
    }

    /**
     * 修改密码
     *
     * @param id                    基础信息主键ID
     * @param userUpdatePasswordDTO 修改密码实体
     */
    @Override
    public void updatePassword(Integer id, UserUpdatePasswordDTO userUpdatePasswordDTO) {
        if (Objects.nonNull(id)) {
            BaseUser baseUser = this.baseUserService.getById(id);
            if (Objects.isNull(baseUser)) {
                throw new BusinessException("用户信息查询异常");
            }
            BaseUser baseUserEntity = BaseUser.builder()
                    .id(id)
                    .password(userUpdatePasswordDTO.getPassword())
                    .build();
            this.baseUserService.updateById(baseUserEntity);
        }
    }

    @Override
    public void uploadAvatar(BaseUserUpdateDTO baseUserUpdateDTO) {
        BaseUser baseUserUpdate = this.baseUserService.getById(baseUserUpdateDTO.getId());
        if (Objects.isNull(baseUserUpdate)) {
            throw new BusinessException("用户信息查询异常");
        }
        if (!baseUserUpdateDTO.getAvatar().equals(baseUserUpdate.getAvatar())) {
            this.fileUploadService.deleteFile(baseUserUpdate.getAvatar());
        }
        BaseUser baseUser = new BaseUser();
        baseUser.setId(baseUserUpdateDTO.getId());
        baseUser.setAvatar(baseUserUpdateDTO.getAvatar());
        this.baseUserService.updateById(baseUser);
    }

    /**
     * 修改个人信息
     *
     * @param baseUserUpdateDTO 修改参数DTO
     */
    @Override
    public void updateInfo(BaseUserUpdateDTO baseUserUpdateDTO) {
        BaseUser baseUserUpdate = this.baseUserService.getById(baseUserUpdateDTO.getId());
        if (Objects.isNull(baseUserUpdate)) {
            throw new BusinessException("用户信息查询异常");
        }
        // 使用 UpdateWrapper 只更新非空字段
        LambdaUpdateWrapper<BaseUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BaseUser::getId, baseUserUpdateDTO.getId());

        if (StringUtils.hasText(baseUserUpdateDTO.getUsername())) {
            updateWrapper.set(BaseUser::getUsername, baseUserUpdateDTO.getUsername());
        }
        if (StringUtils.hasText(baseUserUpdateDTO.getPhone())) {
            updateWrapper.set(BaseUser::getPhone, baseUserUpdateDTO.getPhone());
        }
        if (baseUserUpdateDTO.getGender() != null) {
            updateWrapper.set(BaseUser::getGender, baseUserUpdateDTO.getGender());
        }
        // 只有明确传入了avatar才更新，否则保持原样
        if (baseUserUpdateDTO.getAvatar() != null) {
            updateWrapper.set(BaseUser::getAvatar, baseUserUpdateDTO.getAvatar());
        }

        this.baseUserService.update(updateWrapper);
    }

    /**
     * 列表查询
     *
     * @param baseUserQueryDTO 查询条件类
     * @return List<BaseUserVO>
     */
    @Override
    public List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO) {
        List<User> list = this.list(new LambdaQueryWrapper<>());
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<Integer> baseUserIds = list.stream().map(User::getBaseUserId).toList();
        baseUserQueryDTO.setIds(baseUserIds);
        this.baseUserService.listPage(baseUserQueryDTO);
        return this.baseUserService.listPage(baseUserQueryDTO);
    }

    @Override
    public Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO) {
        return this.baseUserService.listPageCount(baseUserQueryDTO);
    }

    /**
     * 用户新增
     *
     * @param baseUser 基础用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(BaseUser baseUser) {
        baseUser.setCreateTime(LocalDateTime.now());
        baseUser.setLoginStatus(AccountStatusEnum.NORMAL.getStatusCode());
        // 基础用户信息
        this.baseUserService.save(baseUser);
        // 用户信息
        User user = User.builder()
                .id(baseUser.getId())
                .baseUserId(baseUser.getId())
                .build();
        this.save(user);
    }

    /**
     * 用户信息修改
     *
     * @param baseUser 基础用户信息
     */
    @Override
    public void updateAdminInfo(BaseUser baseUser) {
        this.baseUserService.updateById(baseUser);
    }

    /**
     * 删除用户信息
     *
     * @param id 基础用户信息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        this.baseUserService.removeById(id);
        this.remove(
                new LambdaUpdateWrapper<User>()
                        .eq(User::getBaseUserId, id)
        );
    }

    /**
     * 通过主键ID查询用户详情
     *
     * @param baseUserId 基础用户信息ID
     * @return BaseUserVO
     */
    @Override
    public BaseUserVO detail(Integer baseUserId) {
        if (Objects.nonNull(baseUserId)) {
            BaseUserQueryDTO baseUserQueryDTO = new BaseUserQueryDTO();
            baseUserQueryDTO.setId(baseUserId);
            List<BaseUserVO> baseUserVOS = this.baseUserService.listPage(baseUserQueryDTO);
            if (CollectionUtils.isEmpty(baseUserVOS)) {
                return null;
            }
            return baseUserVOS.stream().findFirst().orElse(null);
        }
        return null;
    }
}