package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.ProductImageMapper;
import com.example.mapper.ProductMapper;
import com.example.mapper.CategoryMapper;
import com.example.mapper.OrderMapper;
import com.example.pojo.entity.Category;
import com.example.pojo.entity.Order;
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
import jakarta.validation.Validator;
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
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final Validator validator;

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
        normalizeQuery(queryDTO);
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
        normalizeQuery(queryDTO);
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
        if (userId == null) throw new BusinessException("请先登录再发布商品", 400);
        requireEnabledCategory(product.getCategoryId());
        List<ProductImageDTO> images = validateAndNormalizeImages(addDTO.getImages(), userId);
        product.setTitle(product.getTitle().trim());
        if (!this.save(product)) throw new BusinessException("发布失败，请稍后重试", 409);

        // 保存图册
        saveProductImages(product.getId(), images);
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
            throw new BusinessException("商品不存在", 404);
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能修改自己发布的商品", 403);
        }

        if (updateDTO.getVersion() != null && !Objects.equals(updateDTO.getVersion(), product.getVersion())) {
            throw new BusinessException("商品已被其他操作更新，请刷新后重新编辑", 409);
        }
        if (updateDTO.getCategoryId() != null) {
            requireEnabledCategory(updateDTO.getCategoryId());
            product.setCategoryId(updateDTO.getCategoryId());
        }
        if (updateDTO.getTitle() != null) product.setTitle(updateDTO.getTitle().trim());
        if (updateDTO.getDescription() != null) product.setDescription(updateDTO.getDescription());
        if (updateDTO.getPrice() != null) product.setPrice(updateDTO.getPrice());
        if (Boolean.TRUE.equals(updateDTO.getClearOriginalPrice())) product.setOriginalPrice(null);
        else if (updateDTO.getOriginalPrice() != null) product.setOriginalPrice(updateDTO.getOriginalPrice());
        if (updateDTO.getCondition() != null) product.setCondition(updateDTO.getCondition());
        if (updateDTO.getTradeType() != null) product.setTradeType(updateDTO.getTradeType());
        if (updateDTO.getStock() != null) product.setStock(updateDTO.getStock());
        validateMergedProduct(product);
        List<ProductImageDTO> images = updateDTO.getImages() == null ? null : validateAndNormalizeImages(updateDTO.getImages(), userId);

        // 内容修改后重新审核；已下架商品保持下架，明确上架时再提交审核。
        int nextStatus = Objects.equals(product.getStatus(), STATUS_DOWN) ? STATUS_DOWN : STATUS_PENDING;
        int version = product.getVersion() == null ? 0 : product.getVersion();
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, product.getId()).eq(Product::getUserId, userId)
                .eq(Product::getVersion, version).eq(Product::getStatus, product.getStatus())
                .set(Product::getCategoryId, product.getCategoryId())
                .set(Product::getTitle, product.getTitle()).set(Product::getDescription, product.getDescription())
                .set(Product::getPrice, product.getPrice()).set(Product::getOriginalPrice, product.getOriginalPrice())
                .set(Product::getCondition, product.getCondition()).set(Product::getTradeType, product.getTradeType())
                .set(Product::getStock, product.getStock()).set(Product::getStatus, nextStatus)
                .set(Product::getAuditRemark, null).set(Product::getAuditTime, null).set(Product::getAuditorId, null)
                .set(Product::getUpdateTime, LocalDateTime.now()).set(Product::getVersion, version + 1);
        if (!this.update(wrapper)) throw new BusinessException("商品已变化，请刷新后重新编辑", 409);
        if (images != null) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, product.getId()));
            saveProductImages(product.getId(), images);
        }
    }

    /**
     * 管理员审核商品
     */
    @Override
    public void audit(Integer auditorId, ProductAuditDTO auditDTO) {
        Product product = this.getById(auditDTO.getId());
        if (product == null) {
            throw new BusinessException("商品不存在", 404);
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
            throw new BusinessException("商品不存在", 404);
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能下架自己发布的商品", 403);
        }
        if (product.getStatus() == STATUS_DOWN) {
            throw new BusinessException("商品已下架");
        }

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        int version = product.getVersion() == null ? 0 : product.getVersion();
        wrapper.eq(Product::getId, productId).eq(Product::getUserId, userId)
                .eq(Product::getVersion, version).eq(Product::getStatus, product.getStatus())
                .set(Product::getStatus, STATUS_DOWN).set(Product::getVersion, version + 1)
                .set(Product::getUpdateTime, LocalDateTime.now());
        if (!this.update(wrapper)) throw new BusinessException("商品状态已变化，请刷新后重试", 409);
    }

    /**
     * 用户重新上架已下架的商品
     * <p>状态回到「待审核」，需重新经过管理员审核</p>
     */
    @Override
    public void up(Integer userId, Integer productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在", 404);
        }
        if (!Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能操作自己发布的商品", 403);
        }
        if (product.getStatus() != STATUS_DOWN) {
            throw new BusinessException("只有已下架的商品才能重新上架");
        }

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        int version = product.getVersion() == null ? 0 : product.getVersion();
        wrapper.eq(Product::getId, productId).eq(Product::getUserId, userId)
                .eq(Product::getVersion, version).eq(Product::getStatus, STATUS_DOWN)
                .set(Product::getVersion, version + 1).set(Product::getUpdateTime, LocalDateTime.now())
                .set(Product::getStatus, STATUS_PENDING)
                .set(Product::getAuditRemark, null)
                .set(Product::getAuditTime, null)
                .set(Product::getAuditorId, null);
        if (!this.update(wrapper)) throw new BusinessException("商品状态已变化，请刷新后重试", 409);
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
            throw new BusinessException("商品不存在", 404);
        }
        if (!isAdmin && !Objects.equals(product.getUserId(), userId)) {
            throw new BusinessException("只能删除自己发布的商品", 403);
        }

        // 订单模块拥有订单生命周期；商品模块只读检查，避免删除后订单详情断链。
        if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getProductId, productId)) > 0) {
            throw new BusinessException("该商品已有订单，请下架保留交易记录", 409);
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

    /** 请求字段约束由 Controller 的 @Valid 执行；这里仅整理搜索词。 */
    private void normalizeQuery(ProductQueryDTO queryDTO) {
        if (queryDTO.getTitle() != null) queryDTO.setTitle(queryDTO.getTitle().trim());
    }

    /** 局部编辑合并后，复用发布 DTO 的字段约束检查完整商品，不另写一套金额/长度规则。 */
    private void validateMergedProduct(Product product) {
        validateProductField("title", product.getTitle());
        validateProductField("description", product.getDescription());
        validateProductField("price", product.getPrice());
        validateProductField("originalPrice", product.getOriginalPrice());
        validateProductField("categoryId", product.getCategoryId());
        validateProductField("condition", product.getCondition());
        validateProductField("tradeType", product.getTradeType());
        validateProductField("stock", product.getStock());
    }

    private void validateProductField(String field, Object value) {
        validator.validateValue(ProductAddDTO.class, field, value).stream()
                .map(violation -> violation.getMessage()).sorted().findFirst()
                .ifPresent(message -> { throw new BusinessException(message, 400); });
    }

    /** 发布和编辑复用：检查图册组合规则，返回统一封面和顺序的新列表。 */
    private List<ProductImageDTO> validateAndNormalizeImages(List<ProductImageDTO> input, Integer userId) {
        // 保留结构保护，避免内部调用遇到空列表/空元素时产生空指针异常。
        if (input == null || input.isEmpty() || input.size() > 9) {
            throw new BusinessException("请保留1至9张商品图片", 400);
        }
        Set<String> urls = new HashSet<>();
        int cover = -1;
        List<ProductImageDTO> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ProductImageDTO image = input.get(i);
            if (image == null || image.getImageUrl() == null) {
                throw new BusinessException("图片地址不能为空", 400);
            }
            String url = image.getImageUrl().trim();
            if (!url.matches("(?i)^/files/(?:products/[1-9][0-9]*/)?[a-z0-9_-]+\\.(png|jpe?g)$")) {
                throw new BusinessException("请使用本站上传的JPG/PNG图片", 400);
            }
            if (url.startsWith("/files/products/")
                    && (userId == null || !url.startsWith("/files/products/" + userId + "/"))) {
                throw new BusinessException("不能使用其他用户上传的商品图片", 400);
            }
            if (!urls.add(url)) throw new BusinessException("请勿重复添加同一张图片", 400);
            if (image.getIsCover() != null && image.getIsCover() != 0 && image.getIsCover() != 1) {
                throw new BusinessException("封面标记无效", 400);
            }
            if (Objects.equals(image.getIsCover(), 1)) {
                if (cover != -1) throw new BusinessException("只能设置一张封面图片", 400);
                cover = i;
            }
            result.add(ProductImageDTO.builder().imageUrl(url).sort(i).isCover(0).build());
        }
        result.get(cover == -1 ? 0 : cover).setIsCover(1);
        return result;
    }

    private void requireEnabledCategory(Integer categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException("分类不存在或已停用，请重新选择", 400);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 保存商品图册
     *
     * @param productId 商品ID
     * @param imageDTOs 图片DTO列表
     */
    private void saveProductImages(Integer productId, List<ProductImageDTO> imageDTOs) {
        List<ProductImage> images = imageDTOs.stream().map(dto ->
                ProductImage.builder()
                        .productId(productId)
                        .imageUrl(dto.getImageUrl())
                        .isCover(dto.getIsCover() != null ? dto.getIsCover() : 0)
                        .sort(dto.getSort() != null ? dto.getSort() : 0)
                        .createTime(LocalDateTime.now())
                        .build()
        ).collect(Collectors.toList());

        // MyBatis Plus 逐条插入
        images.forEach(productImageMapper::insert);
    }
}
