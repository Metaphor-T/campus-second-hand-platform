package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品图册持久化接口
 * <p>全量使用 MyBatis Plus BaseMapper</p>
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}
