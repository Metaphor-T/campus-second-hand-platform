package com.example.product;

import com.example.exception.BusinessException;
import com.example.mapper.OrderMapper;
import com.example.pojo.dto.OrderCreateDTO;
import com.example.pojo.entity.Order;
import com.example.pojo.entity.Product;
import com.example.pojo.vo.OrderVO;
import com.example.service.ProductService;
import com.example.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderSafetyTest {
    OrderMapper orders;
    ProductService products;
    OrderServiceImpl service;
    @BeforeEach void setup() {
        orders = mock(OrderMapper.class);
        products = mock(ProductService.class);
        service = new OrderServiceImpl(orders, products, mock(com.example.mapper.AddressMapper.class));
        ReflectionTestUtils.setField(service, "baseMapper", orders);
    }
    Order order(Integer deducted) {
        return Order.builder().id(1).productId(10).buyerId(20).sellerId(30)
                .quantity(2).stockDeductedQuantity(deducted).orderStatus(0).build();
    }
    @Test void detailOnlyAllowsBuyerOrSeller() {
        when(orders.getDetail(1)).thenReturn(OrderVO.builder().id(1).buyerId(20).sellerId(30).build());
        assertNotNull(service.getDetail(1, 20));
        assertNotNull(service.getDetail(1, 30));
        assertEquals(403, assertThrows(BusinessException.class, () -> service.getDetail(1, 40)).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> service.getDetail(1, null)).getCode());
    }
    @Test void cannotUseAnotherUsersAddressWhenCreatingOrder() {
        var addresses=mock(com.example.mapper.AddressMapper.class);
        var guarded=new OrderServiceImpl(orders,products,addresses);
        when(products.getById(10)).thenReturn(Product.builder().id(10).userId(30).status(1).tradeType(2).build());
        when(addresses.selectById(99)).thenReturn(com.example.pojo.entity.Address.builder().id(99).userId(99).build());
        var dto=new OrderCreateDTO();dto.setProductId(10);dto.setAddressId(99);
        assertEquals(403,assertThrows(BusinessException.class,()->guarded.createOrder(20,dto)).getCode());
        verify(products,never()).deductStock(anyInt(),anyInt(),anyInt());verify(orders,never()).insert(any(Order.class));
    }
    @Test void deletedAddressRejectedBeforeCreatingOrder() {
        var addresses=mock(com.example.mapper.AddressMapper.class);var guarded=new OrderServiceImpl(orders,products,addresses);
        when(products.getById(10)).thenReturn(Product.builder().id(10).userId(30).status(1).tradeType(2).build());
        var dto=new OrderCreateDTO();dto.setProductId(10);dto.setAddressId(99);
        assertEquals(400,assertThrows(BusinessException.class,()->guarded.createOrder(20,dto)).getCode());verify(orders,never()).insert(any(Order.class));
    }
    @Test void cancellationOfUnlimitedOrderDoesNotRestoreInventory() {
        Order o = order(0);
        when(orders.selectForUpdate(1)).thenReturn(o);
        service.cancelOrder(20, 1, "test");
        verify(products, never()).restoreStock(anyInt(), anyInt());
        assertEquals(4, o.getOrderStatus());
    }
    @Test void finiteCancellationRestoresActualDeductionOnlyOnce() {
        Order o = order(2);
        when(orders.selectForUpdate(1)).thenReturn(o);
        service.cancelOrder(20, 1, "test");
        assertThrows(BusinessException.class, () -> service.cancelOrder(20, 1, "repeat"));
        verify(products, times(1)).restoreStock(10, 2);
        assertEquals(0, o.getStockDeductedQuantity());
    }
    @Test void unknownLegacyDeductionBlocksBeforeAnyInventoryOrStateChange() {
        Order o = order(null);
        when(orders.selectForUpdate(1)).thenReturn(o);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.cancelOrder(20, 1, "test")).getCode());
        verify(products, never()).restoreStock(anyInt(), anyInt());
        verify(orders, never()).updateById(any(Order.class));
        assertEquals(0, o.getOrderStatus());
    }
    @Test void sellerAndAdminRefundUseDeductionSnapshot() {
        Order o = order(0); o.setOrderStatus(5);
        when(orders.selectForUpdate(1)).thenReturn(o);
        service.agreeRefundOrder(30, 1);
        verify(products, never()).restoreStock(anyInt(), anyInt());
        o = order(2); o.setOrderStatus(5);
        when(orders.selectForUpdate(1)).thenReturn(o);
        service.adminAgreeRefundOrder(99, 1);
        verify(products).restoreStock(10, 2);
    }
    @Test void creationRecordsWhetherStockWasActuallyDeducted() {
        when(products.getById(10)).thenReturn(Product.builder().id(10).userId(30).status(1)
                .stock(0).price(new BigDecimal("12.34")).tradeType(1).version(0).build());
        when(orders.insert(any(Order.class))).thenAnswer(inv -> { Order o = inv.getArgument(0); o.setId(1); assertEquals(0, o.getStockDeductedQuantity()); return 1; });
        OrderCreateDTO dto = new OrderCreateDTO(); dto.setProductId(10); dto.setQuantity(2);
        service.createOrder(20, dto);
        verify(products, never()).deductStock(anyInt(), anyInt(), anyInt());
    }
    @Test void finiteCreationPersistsExactDeduction() {
        when(products.getById(10)).thenReturn(Product.builder().id(10).userId(30).status(1)
                .stock(5).price(new BigDecimal("12.34")).tradeType(1).version(0).build());
        when(products.deductStock(10, 2, 0)).thenReturn(true);
        when(orders.insert(any(Order.class))).thenAnswer(inv -> { Order o = inv.getArgument(0); o.setId(1); assertEquals(2, o.getStockDeductedQuantity()); return 1; });
        OrderCreateDTO dto = new OrderCreateDTO(); dto.setProductId(10); dto.setQuantity(2);
        service.createOrder(20, dto);
    }
}
