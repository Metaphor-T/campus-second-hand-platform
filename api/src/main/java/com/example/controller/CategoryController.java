package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.CategoryAddDTO;
import com.example.pojo.dto.CategoryQueryDTO;
import com.example.pojo.dto.CategorySortDTO;
import com.example.pojo.dto.CategoryUpdateDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.CategoryVO;
import com.example.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理控制器
 */
@RestController
@RequestMapping(value = "/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分页查询商品分类列表
     *
     * @param queryDTO 查询参数（支持名称模糊搜索、状态筛选等）
     * @return 分页列表
     */
    @Auth({RoleEnum.ADMIN})
    @Pager
    @PostMapping(value = "/listPage")
    public Result<List<CategoryVO>> listPage(@RequestBody CategoryQueryDTO queryDTO) {
        List<CategoryVO> list = categoryService.listPage(queryDTO);
        Integer count = categoryService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 查询所有商品分类列表（扁平列表，用于前端构建树形结构）
     *
     * @return 全部分类列表
     */
    @Auth({RoleEnum.ADMIN,RoleEnum.USER})
    @GetMapping(value = "/listAll")
    public Result<List<CategoryVO>> listAll() {
        List<CategoryVO> list = categoryService.listAll();
        return Result.success(list);
    }

    /**
     * 批量更新商品分类排序（拖拽排序后调用）
     *
     * @param sortList 排序列表
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/updateSort")
    public Result<Void> updateSort(@RequestBody List<CategorySortDTO> sortList) {
        categoryService.updateSort(sortList);
        return Result.success();
    }

    /**
     * 新增商品分类
     *
     * @param addDTO 新增参数
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PostMapping(value = "/add")
    public Result<Void> add(@Valid @RequestBody CategoryAddDTO addDTO) {
        categoryService.add(addDTO);
        return Result.success();
    }

    /**
     * 修改商品分类
     *
     * @param updateDTO 修改参数
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/update")
    public Result<Void> update(@Valid @RequestBody CategoryUpdateDTO updateDTO) {
        categoryService.update(updateDTO);
        return Result.success();
    }

    /**
     * 删除商品分类
     *
     * @param id 分类主键ID
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @DeleteMapping(value = "/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        categoryService.deleteById(id);
        return Result.success();
    }
}
