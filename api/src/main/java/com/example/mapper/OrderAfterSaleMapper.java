package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.OrderAfterSale;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderAfterSaleMapper extends BaseMapper<OrderAfterSale> {
    @Select("SELECT * FROM order_after_sale WHERE order_id=#{orderId} ORDER BY id DESC")
    List<OrderAfterSale> history(@Param("orderId") Integer orderId);

    @Select("SELECT * FROM order_after_sale WHERE id=#{id} FOR UPDATE")
    OrderAfterSale lock(@Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM order_after_sale WHERE order_id=#{orderId} AND state IN ('WAIT_SELLER','WAIT_ADMIN','WAIT_RETURN')")
    int activeCount(@Param("orderId") Integer orderId);
}
