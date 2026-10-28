package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.ProductAddDTO;
import com.example.pojo.dto.ProductAuditDTO;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.dto.ProductUpdateDTO;
import com.example.pojo.entity.Product;
import com.example.pojo.vo.ProductVO;

import java.util.List;

/**
 * 商品业务逻辑接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 分页查询商品列表（管理员端）
     *
     * @param queryDTO 查询参数
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
     * 用户发布商品（含图册）
     *
     * @param userId 发布者ID
     * @param addDTO 商品信息
     */
    void add(Integer userId, ProductAddDTO addDTO);

    /**
     * 用户修改商品（仅本人可修改，含图册全量替换）
     *
     * @param userId    操作者ID
     * @param updateDTO 修改参数
     */
    void update(Integer userId, ProductUpdateDTO updateDTO);

    /**
     * 管理员审核商品（通过/拒绝）
     *
     * @param auditorId 审核管理员ID
     * @param auditDTO  审核参数
     */
    void audit(Integer auditorId, ProductAuditDTO auditDTO);

    /**
     * 用户下架自己的商品
     *
     * @param userId    操作者ID
     * @param productId 商品ID
     */
    void down(Integer userId, Integer productId);

    /**
     * 用户重新上架已下架的商品（状态回到待审核）
     *
     * @param userId    操作者ID
     * @param productId 商品ID
     */
    void up(Integer userId, Integer productId);

    /**
     * 删除商品（管理员可删任意，用户仅删自己的）
     *
     * @param userId    操作者ID（0 表示管理员操作）
     * @param productId 商品ID
     * @param isAdmin   是否为管理员操作
     */
    void deleteById(Integer userId, Integer productId, boolean isAdmin);

/**
 * 查询单个商品详情（联表 user、category，含图册）
 *
 * @param id 商品ID
 * @return 商品VO
 */
ProductVO getDetail(Integer id);

/**
 * 查询热门商品列表（前5条）
 * <p>热度评分 = view_count * 1 + want_count * 3 + favorite_count * 5 + review_count * 2 + rating * 10</p>
 *
 * @return 热门商品VO列表
 */
List<ProductVO> listHotProducts();

/**
 * 扣减商品库存（乐观锁防超卖）
 *
 * @param productId 商品ID
 * @param quantity  扣减数量
 * @param version   当前版本号
 * @return true-扣减成功，false-库存不足或版本冲突
 */
boolean deductStock(Integer productId, Integer quantity, Integer version);

/**
 * 归还商品库存（订单取消/退款时回滚）
 *
 * @param productId 商品ID
 * @param quantity  归还数量
 */
void restoreStock(Integer productId, Integer quantity);
}
