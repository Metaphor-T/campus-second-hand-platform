package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ReviewImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价图片持久化接口
 * <p>全量使用 MyBatis Plus BaseMapper</p>
 */
@Mapper
public interface ReviewImageMapper extends BaseMapper<ReviewImage> {
}
