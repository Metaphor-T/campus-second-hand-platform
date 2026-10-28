package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员持久化接口
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

}
