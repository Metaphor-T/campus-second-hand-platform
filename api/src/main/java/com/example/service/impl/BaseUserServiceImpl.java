package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mapper.BaseUserMapper;
import com.example.pojo.dto.BaseUserQueryDTO;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.BaseUserVO;
import com.example.service.BaseUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基础用户信息业务逻辑接口实现类
 **/
@Service
public class BaseUserServiceImpl extends ServiceImpl<BaseUserMapper, BaseUser>implements BaseUserService {

    @Override
    public List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO) {
        return this.baseMapper.listPage(baseUserQueryDTO);
    }

    @Override
    public Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO) {
        return this.baseMapper.listPageCount(baseUserQueryDTO);
    }
}
