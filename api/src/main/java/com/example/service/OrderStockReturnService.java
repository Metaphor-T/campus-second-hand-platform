package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.ProductMapper;
import com.example.pojo.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 调用者须先锁定订单，随后锁定商品；当前库存模式不改变旧订单的扣库事实。 */
@Service
@RequiredArgsConstructor
public class OrderStockReturnService {
    private final ProductMapper products;

    public int release(Order order, boolean allowUnknownLegacy) {
        Integer quantity = order.getStockDeductedQuantity();
        if (quantity == null) {
            if (!allowUnknownLegacy) throw new BusinessException("历史订单缺少库存扣减记录，请先核对后处理",409);
            order.setStockReleaseDisposition("UNKNOWN_DEDUCTION");
            return 0;
        }
        if (quantity < 0 || quantity > ProductStock.MAX) throw new BusinessException("历史扣库数量异常，请先核对",409);
        if (quantity == 0 && order.getStockReleaseDisposition() != null) return 0;
        int credited = 0;
        String disposition = "NO_DEDUCTION";
        if (quantity > 0) {
            var product = products.lockForStockReturn(order.getProductId());
            if (product == null) disposition = "PRODUCT_MISSING_RECORDED";
            else if (Integer.valueOf(1).equals(product.getStockMode())) disposition = "UNLIMITED_RECORDED";
            else if (!Integer.valueOf(0).equals(product.getStockMode())) disposition = "MODE_UNCONFIRMED_RECORDED";
            else if (product.getStock() == null || product.getStock() < 0 || product.getStock() > ProductStock.MAX - quantity) {
                disposition = "LIMIT_RECORDED";
            } else {
                if (products.restoreStock(product.getId(),quantity) != 1) throw new BusinessException("库存发生变化，请刷新后重试",409);
                credited = quantity;
                disposition = "CREDITED";
            }
        }
        order.setStockReleasedQuantity((order.getStockReleasedQuantity()==null?0:order.getStockReleasedQuantity()) + quantity);
        order.setStockReleaseDisposition(disposition);
        order.setStockDeductedQuantity(0);
        return credited;
    }
}
