package com.example.controller;

import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.ProductQueryDTO;
import com.example.pojo.vo.CategoryVO;
import com.example.pojo.vo.ProductVO;
import com.example.service.CategoryService;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游客端 - 商品浏览控制器
 * <p>提供游客无需登录即可浏览的公开商品相关接口：商品市场页（瀑布流）、商品详情、热门推荐、分类导航等</p>
 *
 * @author campus-system
 */
@RestController
@RequestMapping("/guest/product")
@RequiredArgsConstructor
public class GuestProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 商品市场页数据（含分类导航 + 热门推荐 + 瀑布流列表）
     * <p>一次性返回首页所需全部数据，减少游客端请求次数</p>
     *
     * @param queryDTO 查询参数（支持分类筛选、标题搜索、分页）
     * @return 市场页组合数据
     */
    @PostMapping("/market")
    @Pager
    public Result<Map<String, Object>> getMarketData(@RequestBody ProductQueryDTO queryDTO) {
        Map<String, Object> data = new HashMap<>();

        // 1. 分类导航列表（仅启用状态的分类）
        List<CategoryVO> categories = categoryService.listAll();
        data.put("categories", categories);

        // 2. 热门推荐商品（前5条）
        List<ProductVO> hotProducts = productService.listHotProducts();
        data.put("hotProducts", hotProducts);

        // 3. 瀑布流商品列表（分页，仅审核通过 status=1）
        queryDTO.setStatus(1);
        List<ProductVO> productList = productService.listPage(queryDTO);
        data.put("productList", productList);

        // 4. 商品总数（用于前端瀑布流加载更多判断）
        Integer total = productService.listPageCount(queryDTO);
        data.put("total", total);

        return Result.success(data);
    }

    /**
     * 商品详情
     *
     * @param id 商品ID
     * @return 商品详情（含图册、发布者信息、分类信息）
     */
    @GetMapping("/detail/{id}")
    public Result<ProductVO> getDetail(@PathVariable Integer id) {
        ProductVO detail = productService.getDetail(id);
        return Result.success(detail);
    }

    /**
     * 热门商品列表
     * <p>用于首页热门推荐区域</p>
     *
     * @return 热门商品列表
     */
    @GetMapping("/hot")
    public Result<List<ProductVO>> getHotProducts() {
        List<ProductVO> hotProducts = productService.listHotProducts();
        return Result.success(hotProducts);
    }
}
