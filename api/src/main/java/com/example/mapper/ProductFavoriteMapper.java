package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ProductFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品收藏持久化接口
 * <p>全量使用 MyBatis Plus BaseMapper + XML 自定义查询</p>
 */
@Mapper
public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {
}
