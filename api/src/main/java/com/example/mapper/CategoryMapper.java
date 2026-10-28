package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.dto.CategoryQueryDTO;
import com.example.pojo.entity.Category;
import com.example.pojo.vo.CategoryVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品分类持久化接口
 * <p>单表简单 CRUD 使用 MyBatis Plus BaseMapper 通用接口</p>
 * <p>分页查询/联表/复杂 SQL 在 CategoryMapper.xml 中实现</p>
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

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
     * @param queryDTO 查询参数（可选过滤条件）
     * @return 全部分类VO列表
     */
    List<CategoryVO> listAll(CategoryQueryDTO queryDTO);
}
