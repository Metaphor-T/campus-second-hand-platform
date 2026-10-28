package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.dto.ProductReviewQueryDTO;
import com.example.pojo.entity.ProductReview;
import com.example.pojo.vo.ProductReviewVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品评价持久化接口
 * <p>单表简单 CRUD 使用 MyBatis Plus BaseMapper</p>
 * <p>分页联表查询在 ProductReviewMapper.xml 中实现</p>
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /**
     * 分页查询商品评价列表（联表 product、base_user）
     *
     * @param queryDTO 查询参数（支持商品/买家/卖家/评分等筛选）
     * @return 评价VO列表
     */
    List<ProductReviewVO> listPage(ProductReviewQueryDTO queryDTO);

    /**
     * 查询评价总数（配合分页）
     *
     * @param queryDTO 查询参数
     * @return 符合条件的记录数
     */
    Integer listPageCount(ProductReviewQueryDTO queryDTO);

    /**
     * 查询单个评价详情（联表 product、base_user）
     *
     * @param id 评价ID
     * @return 评价VO
     */
    ProductReviewVO getDetail(Integer id);

    /**
     * 查询商品的平均评分
     *
     * @param productId 商品ID
     * @return 平均评分
     */
    Double getAvgRatingByProductId(Integer productId);

    /**
     * 查询商品的评价总数
     *
     * @param productId 商品ID
     * @return 评价总数
     */
    Integer getReviewCountByProductId(Integer productId);
}
