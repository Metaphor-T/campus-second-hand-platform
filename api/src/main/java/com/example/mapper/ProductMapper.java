package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.entity.Product;
import com.example.pojo.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * 商品持久化接口
 * <p>单表简单 CRUD 使用 MyBatis Plus BaseMapper</p>
 * <p>分页联表查询在 ProductMapper.xml 中实现</p>
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 分页查询商品列表（联表 user、category）
     *
     * @param queryDTO 查询参数（支持标题/分类/状态/价格等筛选）
     * @return 商品VO列表
     */
    List<ProductVO> listPage(ProductQueryDTO queryDTO);

    /**
     * 查询商品总数（配合分页）
     *
     * @param queryDTO 查询参数
     * @return 符合条件的记录数
     */
    Integer listPageCount(ProductQueryDTO queryDTO);

    /**
     * 查询单个商品详情（联表 user、category）
     *
     * @param id 商品ID
     * @return 商品VO
     */
    ProductVO getDetail(Integer id);

    /**
     * 查询热门商品列表（按热度评分降序，取前N条）
     * <p>热度评分 = view_count * 1 + want_count * 3 + favorite_count * 5 + review_count * 2 + rating * 10</p>
     *
     * @return 热门商品VO列表（最多5条）
     */
    List<ProductVO> listHotProducts();

    /**
     * 扣减库存（乐观锁防超卖）
     *
     * @param id       商品ID
     * @param quantity 扣减数量
     * @param version  当前版本号（乐观锁校验）
     * @return 受影响行数（0 表示库存不足或版本不一致）
     */
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity, @Param("version") Integer version);

    /**
     * 归还库存（订单取消/退款时回滚）
     *
     * @param id       商品ID
     * @param quantity 归还数量
     * @return 受影响行数
     */
    int restoreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
