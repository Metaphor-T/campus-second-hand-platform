package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.AdminMapper;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Admin;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.enums.AccountStatusEnum;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.BaseUserVO;
import com.example.pojo.vo.UserLoginResponseVO;
import com.example.service.AdminService;
import com.example.service.BaseUserService;
import com.example.service.FileUploadService;
import com.example.utils.JwtUtil;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 管理员信息业务逻辑接口实现类
 **/
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

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
        // 回管理员角色表，查询身份是否正确
        Integer baseUserId = baseUser.getId();
        Admin admin = this.getOne(new LambdaQueryWrapper<Admin>().eq(Admin::getBaseUserId, baseUserId));
        if (admin == null) {
            throw new BusinessException("用户不存在");
        }
        // 密码判断
        if (!Objects.equals(baseUser.getPassword(), userLoginDTO.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 封号判断
        if(AccountStatusEnum.FORBIDDEN_LOGIN.getStatusCode().equals(baseUser.getLoginStatus())){
            throw new BusinessException("该账号已被封禁，禁止登录！可联系管理员咨询！");
        }
        // 生成Token
        String token = jwtUtil.generateToken(baseUser.getId(), RoleEnum.ADMIN.getRole(), baseUser.getPassword());
        return UserLoginResponseVO
                .builder()
                .token(token)
                .role(RoleEnum.ADMIN.getRole())
                .build();
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
        if(AccountStatusEnum.FORBIDDEN_LOGIN.getStatusCode().equals(baseUser.getLoginStatus())){
            throw new BusinessException("该账号已被封禁，禁止使用！可联系管理员咨询！");
        }
        return BaseUserVO.builder()
                .id(baseUser.getId())
                .username(baseUser.getUsername())
                .account(baseUser.getAccount())
                .avatar(baseUser.getAvatar())
                .phone(baseUser.getPhone())
                .gender(baseUser.getGender()) // 添加
                .loginStatus(baseUser.getLoginStatus())
                .build();
    }

    /**
     * 列表查询
     *
     * @param baseUserQueryDTO 查询条件类
     * @return List<BaseUserVO>
     */
    @Override
    public List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO) {
        List<Admin> list = this.list(new LambdaQueryWrapper<>());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<Integer> baseUserIds = list.stream().map(Admin::getBaseUserId).toList();
        baseUserQueryDTO.setIds(baseUserIds);
        this.baseUserService.listPage(baseUserQueryDTO);
        return this.baseUserService.listPage(baseUserQueryDTO);
    }

    @Override
    public Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO) {
        return this.baseUserService.listPageCount(baseUserQueryDTO);
    }

    /**
     * 管理员新增
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
        // 管理员信息
        Admin admin = Admin.builder()
                .id(baseUser.getId())
                .baseUserId(baseUser.getId())
                .build();
        this.save(admin);
    }

    /**
     * 管理员信息修改
     *
     * @param baseUser 基础用户信息
     */
    @Override
    public void updateAdminInfo(BaseUser baseUser) {
        this.baseUserService.updateById(baseUser);
    }

    /**
     * 删除管理员信息
     *
     * @param id 基础用户信息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        // 限制：为了系统的正常使用，至少留有一个管理员
        long count = this.count();
        if (count <= 1) {
            throw new BusinessException("为了系统的正常使用，请至少保留一个管理员");
        }
        this.baseUserService.removeById(id);
        this.remove(
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getBaseUserId, id)
        );
    }

    /**
     * 通过主键ID查询管理员详情
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



