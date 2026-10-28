package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ItemSimilarity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品相似度持久化接口
 * <p>使用 MyBatis Plus BaseMapper 提供通用 CRUD</p>
 */
@Mapper
public interface ItemSimilarityMapper extends BaseMapper<ItemSimilarity> {
}
