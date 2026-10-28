package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.ProductImageMapper;
import com.example.mapper.ProductMapper;
import com.example.pojo.dto.ProductAddDTO;
import com.example.pojo.dto.ProductAuditDTO;
import com.example.pojo.dto.ProductImageDTO;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.dto.ProductUpdateDTO;
import com.example.pojo.entity.Product;
import com.example.pojo.entity.ProductImage;
import com.example.pojo.vo.ProductImageVO;
import com.example.pojo.vo.ProductVO;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品业务逻辑接口实现类
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

    /** 审核状态常量 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_DOWN = 3;

    /**
     * 分页查询商品列表（联表 user + category）并附带图册
     */
    @Override
    public List<ProductVO> listPage(ProductQueryDTO queryDTO) {
        List<ProductVO> list = productMapper.listPage(queryDTO);
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 收集所有商品ID
        List<Integer> productIds = list.stream()
                .map(ProductVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return list;
        }

        // 批量查询图册
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSort)
        );

        // 按 productId 分组
        Map<Integer, List<ProductImageVO>> imageMap = new HashMap<>();
        for (ProductImage img : images) {
            imageMap.computeIfAbsent(img.getProductId(), k -> new ArrayList<>())
                    .add(ProductImageVO.builder()
                            .id(img.getId())
                            .productId(img.getProductId())
                            .imageUrl(img.getImageUrl())
                            .isCover(img.getIsCover())
                            .sort(img.getSort())
                            .build());
        }

        // 挂载图册到对应 VO
        for (ProductVO vo : list) {
            vo.setImages(imageMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }

        return list;
    }

    /**
     * 查询商品总数
     */
    @Override
    public Integer listPageCount(ProductQueryDTO queryDTO) {
        return productMapper.listPageCount(queryDTO);
    }

    /**
     * 用户发布商品（含图册）
     * <p>新发布商品状态默认为「待审核」</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer userId, ProductAddDTO addDTO) {
        // 构建商品实体
        Product product = Product.builder()
                .userId(userId)
                .categoryId(addDTO.getCategoryId())
                .title(addDTO.getTitle())
                .description(addDTO.getDescription())
                .price(addDTO.getPrice())
                .originalPrice(addDTO.getOriginalPrice())
                .condition(addDTO.getCondition())
                .tradeType(addDTO.getTradeType())
                .stock(addDTO.getStock() != null ? addDTO.getStock() : 0) // 默认0（不限库存）
                .version(0) // 初始版本号
                .status(STATUS_PENDING) // 默认待审核
                .viewCount(0)
                .wantCount(0)
                .favoriteCount(0)
                .createTime(LocalDateTime.now())
                .build();
        this.save(product);

        // 保存图册
        if (addDTO.getImages() != null && !addDTO.getImages().isEmpty()) {
            saveProductImages(product.getId(), addDTO.getImages(), true);
        }
    }

    /**
     * 用户修改商品（仅本人可修改）
     * <p>图片全量替换：先删旧图，再插入新图</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer userId, ProductUpdateDTO updateDTO) {
        Product product = this.getById(updateDTO.getId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能修改自己发布的商品");
        }

        // 使用 UpdateWrapper 仅更新非空字段
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, updateDTO.getId());
        wrapper.eq(Product::getUserId, userId); // 双重校验归属

        if (updateDTO.getCategoryId() != null) wrapper.set(Product::getCategoryId, updateDTO.getCategoryId());
        if (StringUtils.hasText(updateDTO.getTitle())) wrapper.set(Product::getTitle, updateDTO.getTitle());
        if (updateDTO.getDescription() != null) wrapper.set(Product::getDescription, updateDTO.getDescription());
        if (updateDTO.getPrice() != null) wrapper.set(Product::getPrice, updateDTO.getPrice());
        if (updateDTO.getOriginalPrice() != null) wrapper.set(Product::getOriginalPrice, updateDTO.getOriginalPrice());
        if (updateDTO.getCondition() != null) wrapper.set(Product::getCondition, updateDTO.getCondition());
        if (updateDTO.getTradeType() != null) wrapper.set(Product::getTradeType, updateDTO.getTradeType());
        if (updateDTO.getStock() != null) wrapper.set(Product::getStock, updateDTO.getStock());

        // 修改后状态重置为待审核（除非当前已是审核通过）
        if (product.getStatus() != null && product.getStatus() != STATUS_APPROVED) {
            wrapper.set(Product::getStatus, STATUS_PENDING);
        }

        this.update(wrapper);

        // 图片全量替换
        if (updateDTO.getImages() != null) {
            // 删除旧图
            productImageMapper.delete(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, updateDTO.getId())
            );
            // 插入新图
            if (!updateDTO.getImages().isEmpty()) {
                saveProductImages(updateDTO.getId(), updateDTO.getImages(), false);
            }
        }
    }

    /**
     * 管理员审核商品
     */
    @Override
    public void audit(Integer auditorId, ProductAuditDTO auditDTO) {
        Product product = this.getById(auditDTO.getId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStatus() != STATUS_PENDING) {
            throw new BusinessException("该商品不是待审核状态，无需重复审核");
        }
        if (auditDTO.getStatus() != STATUS_APPROVED && auditDTO.getStatus() != STATUS_REJECTED) {
            throw new BusinessException("审核结果只能是 1-通过 或 2-拒绝");
        }

        // 更新审核信息
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, auditDTO.getId())
                .set(Product::getStatus, auditDTO.getStatus())
                .set(Product::getAuditTime, LocalDateTime.now())
                .set(Product::getAuditorId, auditorId);

        // 拒绝时填写备注
        if (auditDTO.getStatus().equals(STATUS_REJECTED) && StringUtils.hasText(auditDTO.getAuditRemark())) {
            wrapper.set(Product::getAuditRemark, auditDTO.getAuditRemark());
        }

        this.update(wrapper);
    }

    /**
     * 用户下架自己的商品
     */
    @Override
    public void down(Integer userId, Integer productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能下架自己发布的商品");
        }
        if (product.getStatus() == STATUS_DOWN) {
            throw new BusinessException("商品已下架");
        }

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId)
                .eq(Product::getUserId, userId)
                .set(Product::getStatus, STATUS_DOWN);
        this.update(wrapper);
    }

    /**
     * 用户重新上架已下架的商品
     * <p>状态回到「待审核」，需重新经过管理员审核</p>
     */
    @Override
    public void up(Integer userId, Integer productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能操作自己发布的商品");
        }
        if (product.getStatus() != STATUS_DOWN) {
            throw new BusinessException("只有已下架的商品才能重新上架");
        }

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId)
                .eq(Product::getUserId, userId)
                .set(Product::getStatus, STATUS_PENDING)
                .set(Product::getAuditRemark, null)
                .set(Product::getAuditTime, null)
                .set(Product::getAuditorId, null);
        this.update(wrapper);
    }

    /**
     * 删除商品（管理员可删任意，用户仅删自己的）
     * <p>同时删除关联图册</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer userId, Integer productId, boolean isAdmin) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!isAdmin && !Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能删除自己发布的商品");
        }

        // 先删图册
        productImageMapper.delete(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
        );
        // 再删商品
        this.removeById(productId);
    }

    /**
     * 查询单个商品详情（联表 user、category，含图册）
     */
    @Override
    public ProductVO getDetail(Integer id) {
        // 联表查询商品基本信息（含用户名、头像、分类名）
        ProductVO vo = productMapper.getDetail(id);
        if (vo == null) {
            return null;
        }

        // 查询该商品的图册
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort)
        );

        // 转换为 VO
        List<ProductImageVO> imageVOs = images.stream().map(img ->
                ProductImageVO.builder()
                        .id(img.getId())
                        .productId(img.getProductId())
                        .imageUrl(img.getImageUrl())
                        .isCover(img.getIsCover())
                        .sort(img.getSort())
                        .build()
        ).collect(Collectors.toList());

        vo.setImages(imageVOs);
        return vo;
    }

    // ========== 热门商品 ==========

    /**
     * 查询热门商品列表（前5条）
     * <p>热度评分 = view_count * 1 + want_count * 3 + favorite_count * 5 + review_count * 2 + rating * 10</p>
     */
    @Override
    public List<ProductVO> listHotProducts() {
        List<ProductVO> list = productMapper.listHotProducts();
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 批量查询图册
        List<Integer> productIds = list.stream()
                .map(ProductVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSort)
        );

        Map<Integer, List<ProductImageVO>> imageMap = new HashMap<>();
        for (ProductImage img : images) {
            imageMap.computeIfAbsent(img.getProductId(), k -> new ArrayList<>())
                    .add(ProductImageVO.builder()
                            .id(img.getId())
                            .productId(img.getProductId())
                            .imageUrl(img.getImageUrl())
                            .isCover(img.getIsCover())
                            .sort(img.getSort())
                            .build());
        }

        for (ProductVO vo : list) {
            vo.setImages(imageMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }

        return list;
    }

    // ========== 库存操作（乐观锁防超卖） ==========

    /**
     * 扣减商品库存（乐观锁防超卖）
     * <p>库存为0表示不限库存，不执行扣减逻辑</p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @param version   当前版本号（乐观锁校验）
     * @return true-扣减成功，false-库存不足或版本冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Integer productId, Integer quantity, Integer version) {
        // 不限库存的商品无需扣减
        if (version == null) {
            return true;
        }
        int affected = productMapper.deductStock(productId, quantity, version);
        return affected > 0;
    }

    /**
     * 归还商品库存（订单取消/退款时回滚）
     *
     * @param productId 商品ID
     * @param quantity  归还数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Integer productId, Integer quantity) {
        productMapper.restoreStock(productId, quantity);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 保存商品图册
     *
     * @param productId 商品ID
     * @param imageDTOs 图片DTO列表
     * @param isNew     是否为新发布（新发布时若无封面，默认第一张为封面）
     */
    private void saveProductImages(Integer productId, List<ProductImageDTO> imageDTOs, boolean isNew) {
        List<ProductImage> images = imageDTOs.stream().map(dto ->
                ProductImage.builder()
                        .productId(productId)
                        .imageUrl(dto.getImageUrl())
                        .isCover(dto.getIsCover() != null ? dto.getIsCover() : 0)
                        .sort(dto.getSort() != null ? dto.getSort() : 0)
                        .createTime(LocalDateTime.now())
                        .build()
        ).collect(Collectors.toList());

        // 新发布且没有指定封面 → 默认第一张为封面
        if (isNew && !images.isEmpty() && images.stream().noneMatch(i -> i.getIsCover() == 1)) {
            images.get(0).setIsCover(1);
        }

        // MyBatis Plus 逐条插入
        images.forEach(productImageMapper::insert);
    }
}
