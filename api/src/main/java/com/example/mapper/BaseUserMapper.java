package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.dto.BaseUserQueryDTO;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.BaseUserVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 基础用户信息持久化接口
 */
@Mapper
public interface BaseUserMapper extends BaseMapper<BaseUser> {

    List<BaseUserVO> listPage(BaseUserQueryDTO baseUserQueryDTO);

    Integer listPageCount(BaseUserQueryDTO baseUserQueryDTO);

}
