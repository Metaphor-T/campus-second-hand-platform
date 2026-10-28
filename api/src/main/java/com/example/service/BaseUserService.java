package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.BaseUserQueryDTO;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.BaseUserVO;

import java.util.List;

/**
 * 基础用户业务逻辑接口
 */
public interface BaseUserService extends IService<BaseUser> {

    List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO);

    Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO);

}
