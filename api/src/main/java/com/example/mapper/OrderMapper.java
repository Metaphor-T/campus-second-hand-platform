package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.dto.OrderQueryDTO;
import com.example.pojo.entity.Order;
import com.example.pojo.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单持久化接口
 * <p>单表简单 CRUD 使用 MyBatis Plus BaseMapper</p>
 * <p>联表等复杂查询在 XML 中编写原生 SQL</p>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询买家订单列表（联表 product、address）
     *
     * @param buyerId  买家ID
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listBuyerOrders(@Param("buyerId") Integer buyerId, @Param("query") OrderQueryDTO queryDTO);

    /**
     * 查询买家订单总数
     *
     * @param buyerId  买家ID
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listBuyerOrdersCount(@Param("buyerId") Integer buyerId, @Param("query") OrderQueryDTO queryDTO);

    /**
     * 分页查询卖家订单列表（联表 product、address）
     *
     * @param sellerId 卖家ID
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listSellerOrders(@Param("sellerId") Integer sellerId, @Param("query") OrderQueryDTO queryDTO);

    /**
     * 查询卖家订单总数
     *
     * @param sellerId 卖家ID
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listSellerOrdersCount(@Param("sellerId") Integer sellerId, @Param("query") OrderQueryDTO queryDTO);

    /**
     * 查询单个订单详情（联表 product、address）
     *
     * @param id 订单ID
     * @return 订单VO
     */
    OrderVO getDetail(@Param("id") Integer id);

    /**
     * 管理员分页查询全部订单（联表 product、base_user、address）
     *
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listAllOrders(@Param("query") OrderQueryDTO queryDTO);

    /**
     * 管理员查询全部订单总数
     *
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listAllOrdersCount(@Param("query") OrderQueryDTO queryDTO);

    /**
     * 统计买家各状态订单数量（按 order_status 分组）
     *
     * @param buyerId 买家ID
     * @return 列表，每项含 status（int）和 count（int）
     */
    List<Map<String, Object>> countBuyerOrdersByStatus(@Param("buyerId") Integer buyerId);

    /**
     * 统计卖家各状态订单数量（按 order_status 分组）
     *
     * @param sellerId 卖家ID
     * @return 列表，每项含 status（int）和 count（int）
     */
    List<Map<String, Object>> countSellerOrdersByStatus(@Param("sellerId") Integer sellerId);
}
