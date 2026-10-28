package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.ProductAddDTO;
import com.example.pojo.dto.ProductAuditDTO;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.dto.ProductUpdateDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.ProductVO;
import com.example.service.ProductService;
import com.example.service.UserInteractionService;
import com.example.utils.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品管理控制器
 * <p>管理员：查看所有商品、审核、删除</p>
 * <p>用户端：发布、修改、下架、删除自己的商品</p>
 */
@RestController
@RequestMapping(value = "/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserInteractionService userInteractionService;

    // ==================== 管理员端接口 ====================

    /**
     * 管理员分页查询所有商品
     *
     * @param queryDTO 查询参数（支持标题/分类/状态/价格等筛选）
     * @return 分页列表
     */
    @Auth({RoleEnum.ADMIN})
    @Pager
    @PostMapping(value = "/listPage")
    public Result<List<ProductVO>> listPage(@RequestBody ProductQueryDTO queryDTO) {
        List<ProductVO> list = productService.listPage(queryDTO);
        Integer count = productService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 管理员审核商品（通过/拒绝）
     *
     * @param auditDTO 审核参数
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/audit")
    public Result<Void> audit(@Valid @RequestBody ProductAuditDTO auditDTO) {
        Integer auditorId = UserContext.getUserId();
        productService.audit(auditorId, auditDTO);
        return Result.success();
    }

    /**
     * 管理员删除商品
     *
     * @param id 商品主键ID
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @DeleteMapping(value = "/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        productService.deleteById(0, id, true);
        return Result.success();
    }

    // ==================== 用户端接口 ====================

    /**
     * 用户查询自己发布的商品（分页）
     *
     * @param queryDTO 查询参数（其中 userId 从 Token 中获取）
     * @return 分页列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/myProducts")
    public Result<List<ProductVO>> myProducts(@RequestBody ProductQueryDTO queryDTO) {
        queryDTO.setUserId(UserContext.getUserId());
        List<ProductVO> list = productService.listPage(queryDTO);
        Integer count = productService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 用户发布商品
     *
     * @param addDTO 商品信息（含图册）
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/add")
    public Result<Void> add(@Valid @RequestBody ProductAddDTO addDTO) {
        Integer userId = UserContext.getUserId();
        productService.add(userId, addDTO);
        return Result.success();
    }

    /**
     * 用户修改商品（只能修改自己的）
     *
     * @param updateDTO 修改参数
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/update")
    public Result<Void> update(@Valid @RequestBody ProductUpdateDTO updateDTO) {
        Integer userId = UserContext.getUserId();
        productService.update(userId, updateDTO);
        return Result.success();
    }

    /**
     * 用户下架商品
     *
     * @param id 商品主键ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/down/{id}")
    public Result<Void> down(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        productService.down(userId, id);
        return Result.success();
    }

    /**
     * 用户重新上架已下架的商品（状态回到待审核）
     *
     * @param id 商品主键ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/up/{id}")
    public Result<Void> up(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        productService.up(userId, id);
        return Result.success();
    }

    /**
     * 用户删除自己发布的商品
     *
     * @param id 商品主键ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @DeleteMapping(value = "/my/{id}")
    public Result<Void> deleteMyProduct(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        productService.deleteById(userId, id, false);
        return Result.success();
    }

    // ==================== 用户交互（收藏/想要） ====================

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     *
     * @param productId 商品ID
     * @return 操作后的收藏状态
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/favorite/toggle/{productId}")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Integer productId) {
        Integer userId = UserContext.getUserId();
        boolean favorited = userInteractionService.toggleFavorite(userId, productId);
        return Result.success(java.util.Map.of("favorited", favorited));
    }

    /**
     * 添加"想要"记录（每个用户只能添加一次）
     *
     * @param productId 商品ID
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/want/{productId}")
    public Result<Void> addWant(@PathVariable Integer productId) {
        Integer userId = UserContext.getUserId();
        userInteractionService.addWant(userId, productId);
        return Result.success();
    }

    /**
     * 批量查询当前用户对指定商品的交互状态（收藏/想要）
     *
     * @param productId 商品ID
     * @return { favorited: boolean, wanted: boolean }
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/interact/status/{productId}")
    public Result<Map<String, Object>> getInteractStatus(@PathVariable Integer productId) {
        Integer userId = UserContext.getUserId();
        boolean favorited = userInteractionService.isFavorited(userId, productId);
        boolean wanted = userInteractionService.isWanted(userId, productId);
        return Result.success(java.util.Map.of("favorited", favorited, "wanted", wanted));
    }

    /**
     * 查询当前用户收藏的商品列表（分页）
     * 用于"我的收藏"页面
     *
     * @param queryDTO 查询参数（仅使用 current/size 做分页）
     * @return 商品VO列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/favorite/list")
    public Result<List<ProductVO>> listFavorites(@RequestBody ProductQueryDTO queryDTO) {
        Integer userId = UserContext.getUserId();
        Integer current = queryDTO.getCurrent();
        Integer size = queryDTO.getSize();
        List<ProductVO> list = userInteractionService.listFavoriteProducts(userId, current, size);
        Integer count = userInteractionService.listFavoriteCount(userId);
        return Result.success(list, count);
    }

    /**
     * 查询单个商品详情（联表 user、category，含图册）
     * 此接口公开，不限制登录状态，用于商品详情页展示
     *
     * @param id 商品ID
     * @return 商品VO（含完整图册）
     */
    @Auth
    @GetMapping(value = "/public/detail/{id}")
    public Result<ProductVO> publicDetail(@PathVariable Integer id) {
        ProductVO vo = productService.getDetail(id);
        if (vo == null) {
            return Result.fail("商品不存在");
        }
        return Result.success(vo);
    }

    /**
     * 记录商品浏览（每次访问详情页时调用）
     * <p>不限制登录状态，未登录用户 userId 传 null</p>
     *
     * @param productId 商品ID
     */
    @Auth
    @PostMapping(value = "/view/{productId}")
    public Result<Void> recordView(@PathVariable Integer productId) {
        Integer userId = UserContext.getUserId();
        userInteractionService.recordView(userId, productId);
        return Result.success();
    }

    // ==================== 热门商品 ====================

    /**
     * 查询热门商品列表（前5条）
     * <p>热度评分规则：浏览量*1 + 想要数*3 + 收藏数*5 + 评价数*2 + 评分*10</p>
     * <p>此接口公开，不限制登录状态，用于首页商品类别上方渲染</p>
     *
     * @return 热门商品列表（最多5条）
     */
    @Auth
    @GetMapping(value = "/public/hot")
    public Result<List<ProductVO>> hotList() {
        List<ProductVO> list = productService.listHotProducts();
        return Result.success(list);
    }

    // ==================== 公开接口 ====================

    /**
     * 公开的商品列表（仅展示审核通过的商品）
     * 用于用户端商品市场/首页展示
     *
     * @param queryDTO 查询参数（支持分类/标题/价格等筛选）
     * @return 分页列表
     */
    @Auth
    @Pager
    @PostMapping(value = "/public/list")
    public Result<List<ProductVO>> publicList(@RequestBody ProductQueryDTO queryDTO) {
        // 仅展示审核通过的商品
        queryDTO.setStatus(1);
        List<ProductVO> list = productService.listPage(queryDTO);
        Integer count = productService.listPageCount(queryDTO);
        return Result.success(list, count);
    }
}
