package com.example.utils;

import com.example.exception.BusinessException;
import com.example.pojo.dto.ProductImageDTO;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.entity.Product;
import java.math.BigDecimal;
import java.util.*;

/** 商品规则集中于本模块，前端校验只负责即时提示，后端始终再次验证。 */
public final class ProductRules {
    private ProductRules() {}
    public static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(message, 400);
    }
    public static void validate(Product p) {
        require(p.getTitle() != null && !p.getTitle().isBlank() && p.getTitle().trim().length() <= 200, "请输入1至200字商品标题");
        require(p.getDescription() == null || p.getDescription().length() <= 2000, "商品描述最多2000字");
        money(p.getPrice(), false, "售价");
        if (p.getOriginalPrice() != null) money(p.getOriginalPrice(), true, "原价");
        require(p.getCategoryId() != null && p.getCategoryId() > 0, "请选择商品分类");
        require(p.getCondition() != null && p.getCondition() >= 1 && p.getCondition() <= 4, "新旧程度无效");
        require(p.getTradeType() != null && p.getTradeType() >= 1 && p.getTradeType() <= 3, "交易方式无效");
        require(p.getStock() == null || (p.getStock() >= 0 && p.getStock() <= 999999), "库存应为0至999999的整数");
    }
    private static void money(BigDecimal value, boolean zeroAllowed, String label) {
        require(value != null && value.compareTo(BigDecimal.ZERO) >= (zeroAllowed ? 0 : 1)
                && value.compareTo(new BigDecimal("99999999.99")) <= 0 && value.stripTrailingZeros().scale() <= 2,
                label + (zeroAllowed ? "应为0至99999999.99元，最多2位小数" : "应为0.01至99999999.99元，最多2位小数"));
    }
    public static void query(ProductQueryDTO q) {
        if (q.getTitle() != null) {
            q.setTitle(q.getTitle().trim());
            require(q.getTitle().length() <= 200, "关键词最多200字");
        }
        if (q.getMinPrice() != null) money(q.getMinPrice(), true, "最低价格");
        if (q.getMaxPrice() != null) money(q.getMaxPrice(), true, "最高价格");
        require(q.isPriceRangeValid(), "最低价格不能高于最高价格");
        require(q.getSortBy() == null || Set.of("latest", "priceAsc", "priceDesc", "hot").contains(q.getSortBy()), "排序方式无效");
    }
    /** 返回新列表：封面唯一；未指定封面时采用首图；排序按实际图册顺序保存。 */
    public static List<ProductImageDTO> images(List<ProductImageDTO> input, Integer userId) {
        require(input != null && !input.isEmpty() && input.size() <= 9, "请保留1至9张商品图片");
        Set<String> urls = new HashSet<>();
        int cover = -1;
        List<ProductImageDTO> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ProductImageDTO img = input.get(i);
            require(img != null && img.getImageUrl() != null, "图片地址不能为空");
            String url = img.getImageUrl().trim();
            require(url.matches("(?i)^/files/(?:products/[1-9][0-9]*/)?[a-z0-9_-]+\\.(png|jpe?g)$"), "请使用本站上传的JPG/PNG图片");
            if (url.startsWith("/files/products/")) {
                require(userId != null && url.startsWith("/files/products/" + userId + "/"), "不能使用其他用户上传的商品图片");
            }
            require(urls.add(url), "请勿重复添加同一张图片");
            require(img.getIsCover() == null || img.getIsCover() == 0 || img.getIsCover() == 1, "封面标记无效");
            if (Objects.equals(img.getIsCover(), 1)) {
                require(cover == -1, "只能设置一张封面图片");
                cover = i;
            }
            result.add(ProductImageDTO.builder().imageUrl(url).sort(i).isCover(0).build());
        }
        result.get(cover == -1 ? 0 : cover).setIsCover(1);
        return result;
    }
}
