package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ProductView;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品浏览记录 Mapper 接口
 * <p>提供 product_view 表的基础 CRUD 操作</p>
 */
@Mapper
public interface ProductViewMapper extends BaseMapper<ProductView> {
}
