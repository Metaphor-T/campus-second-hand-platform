package com.example.controller;

import com.example.pojo.api.Result;
import com.example.pojo.vo.CategoryVO;
import com.example.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 游客端 - 分类浏览控制器
 * <p>提供游客无需登录即可浏览的公开分类相关接口</p>
 *
 * @author campus-system
 */
@RestController
@RequestMapping("/guest/category")
@RequiredArgsConstructor
public class GuestCategoryController {

    private final CategoryService categoryService;

    /**
     * 查询全部分类列表
     * <p>返回所有启用状态的分类，用于商品市场页的分类筛选导航</p>
     *
     * @return 分类列表（按排序权重降序）
     */
    @GetMapping("/list")
    public Result<List<CategoryVO>> listAll() {
        List<CategoryVO> categories = categoryService.listAll();
        return Result.success(categories);
    }
}
