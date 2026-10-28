package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户持久化接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}