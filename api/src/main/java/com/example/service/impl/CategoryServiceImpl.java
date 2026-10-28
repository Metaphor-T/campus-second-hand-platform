package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.CategoryMapper;
import com.example.pojo.dto.CategoryAddDTO;
import com.example.pojo.dto.CategoryQueryDTO;
import com.example.pojo.dto.CategorySortDTO;
import com.example.pojo.dto.CategoryUpdateDTO;
import com.example.pojo.entity.Category;
import com.example.pojo.vo.CategoryVO;
import com.example.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类业务逻辑接口实现类
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 分页查询商品分类列表
     *
     * @param queryDTO 查询参数（支持名称模糊搜索、状态筛选等）
     * @return 分类VO列表
     */
    @Override
    public List<CategoryVO> listPage(CategoryQueryDTO queryDTO) {
        // 分页查询使用 Mapper XML 中的原生 SQL
        return categoryMapper.listPage(queryDTO);
    }

    /**
     * 查询商品分类总数（配合分页）
     *
     * @param queryDTO 查询参数
     * @return 符合条件的记录总数
     */
    @Override
    public Integer listPageCount(CategoryQueryDTO queryDTO) {
        return categoryMapper.listPageCount(queryDTO);
    }

    /**
     * 新增商品分类
     *
     * @param addDTO 新增参数
     */
    @Override
    public void add(CategoryAddDTO addDTO) {
        // 校验：同一父分类下名称不能重复
        Category existing = this.lambdaQuery()
                .eq(Category::getName, addDTO.getName())
                .eq(addDTO.getParentId() != null, Category::getParentId, addDTO.getParentId())
                .one();
        if (existing != null) {
            throw new BusinessException("该分类名称已存在");
        }

        // 构建实体并保存
        Category category = Category.builder()
                .parentId(addDTO.getParentId() != null ? addDTO.getParentId() : 0)
                .name(addDTO.getName())
                .sort(addDTO.getSort() != null ? addDTO.getSort() : 0)
                .status(addDTO.getStatus() != null ? addDTO.getStatus() : 1)
                .createTime(LocalDateTime.now())
                .build();
        this.save(category);
    }

    /**
     * 修改商品分类
     * <p>使用 UpdateWrapper 只更新非空字段</p>
     *
     * @param updateDTO 修改参数
     */
    @Override
    public void update(CategoryUpdateDTO updateDTO) {
        // 校验分类是否存在
        Category category = this.getById(updateDTO.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 使用 UpdateWrapper 仅更新非空字段
        LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Category::getId, updateDTO.getId());

        if (updateDTO.getParentId() != null) {
            updateWrapper.set(Category::getParentId, updateDTO.getParentId());
        }
        if (StringUtils.hasText(updateDTO.getName())) {
            updateWrapper.set(Category::getName, updateDTO.getName());
        }
        if (updateDTO.getSort() != null) {
            updateWrapper.set(Category::getSort, updateDTO.getSort());
        }
        if (updateDTO.getStatus() != null) {
            updateWrapper.set(Category::getStatus, updateDTO.getStatus());
        }

        this.update(updateWrapper);
    }

    /**
     * 查询所有商品分类（扁平列表）
     *
     * @return 全部分类VO列表
     */
    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.listAll(null);
    }

    /**
     * 批量更新商品分类排序（拖拽排序后调用）
     *
     * @param sortList 排序列表
     */
    @Override
    public void updateSort(List<CategorySortDTO> sortList) {
        if (sortList == null || sortList.isEmpty()) {
            return;
        }
        // 批量更新每个分类的排序权重和父分类
        List<Category> categoryList = sortList.stream().map(s -> Category.builder()
                .id(s.getId())
                .parentId(s.getParentId())
                .sort(s.getSort())
                .build()
        ).collect(Collectors.toList());

        this.updateBatchById(categoryList);
    }

    /**
     * 根据ID删除商品分类
     * <p>删除前校验是否有子分类，若有则不允许删除</p>
     *
     * @param id 分类主键ID
     */
    @Override
    public void deleteById(Integer id) {
        // 校验是否存在
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 校验是否有子分类
        long childCount = this.lambdaQuery()
                .eq(Category::getParentId, id)
                .count();
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，请先删除子分类");
        }

        this.removeById(id);
    }
}
