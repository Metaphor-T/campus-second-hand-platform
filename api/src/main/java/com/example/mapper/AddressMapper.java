package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址持久化接口
 * <p>单表简单 CRUD 使用 MyBatis Plus BaseMapper</p>
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
