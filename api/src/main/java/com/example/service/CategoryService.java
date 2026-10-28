package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.CategoryAddDTO;
import com.example.pojo.dto.CategoryQueryDTO;
import com.example.pojo.dto.CategorySortDTO;
import com.example.pojo.dto.CategoryUpdateDTO;
import com.example.pojo.entity.Category;
import com.example.pojo.vo.CategoryVO;

import java.util.List;

/**
 * 商品分类业务逻辑接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 分页查询商品分类列表
     *
     * @param queryDTO 查询参数（支持名称模糊搜索、状态筛选等）
     * @return 分类VO列表
     */
    List<CategoryVO> listPage(CategoryQueryDTO queryDTO);

    /**
     * 查询商品分类总数（配合分页）
     *
     * @param queryDTO 查询参数
     * @return 符合条件的记录总数
     */
    Integer listPageCount(CategoryQueryDTO queryDTO);

    /**
     * 查询所有商品分类（扁平列表，用于前端构建树形结构）
     *
     * @return 全部分类VO列表（按 sort DESC 排序）
     */
    List<CategoryVO> listAll();

    /**
     * 批量更新商品分类排序（拖拽排序后调用）
     *
     * @param sortList 排序列表
     */
    void updateSort(List<CategorySortDTO> sortList);

    /**
     * 新增商品分类
     *
     * @param addDTO 新增参数
     */
    void add(CategoryAddDTO addDTO);

    /**
     * 修改商品分类
     *
     * @param updateDTO 修改参数（仅更新非空字段）
     */
    void update(CategoryUpdateDTO updateDTO);

    /**
     * 根据ID删除商品分类
     *
     * @param id 分类主键ID
     */
    void deleteById(Integer id);
}
