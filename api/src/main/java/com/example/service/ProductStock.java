package com.example.service;

import com.example.exception.BusinessException;

/** Stock mode is independent of quantity and publication state. */
public final class ProductStock {
    public static final int LIMITED = 0, UNLIMITED = 1, MAX = 999999;
    private ProductStock() {}
    public static void validate(Integer mode, Integer stock, boolean requireAvailable) {
        if (mode == null) throw new BusinessException("历史商品库存模式待确认，请卖家编辑商品后再操作",409);
        if (mode != LIMITED && mode != UNLIMITED) throw new BusinessException("库存模式无效",400);
        if (stock == null || stock < 0 || stock > MAX) throw new BusinessException("库存应为0至999999的整数，不能提交过多数量",400);
        if (mode == UNLIMITED && stock != 0) throw new BusinessException("不限库存模式不填写数量，请将数量清为0",400);
        if (requireAvailable && mode == LIMITED && stock == 0) throw new BusinessException("有限库存至少填写1件才能提交上架",400);
    }
}
