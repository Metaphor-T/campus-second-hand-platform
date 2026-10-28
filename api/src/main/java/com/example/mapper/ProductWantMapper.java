package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ProductWant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品想要记录持久化接口
 * <p>全量使用 MyBatis Plus BaseMapper</p>
 */
@Mapper
public interface ProductWantMapper extends BaseMapper<ProductWant> {
}
