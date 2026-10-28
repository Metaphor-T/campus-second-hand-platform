package com.example.mapper;

import com.example.pojo.vo.UserBehaviorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐系统持久化接口
 * <p>查询用户在各交互表中的行为数据，供协同过滤算法使用</p>
 */
@Mapper
public interface RecommendMapper {

    /**
     * 查询单个用户的所有行为记录（收藏/想要/评价/下单）
     * <p>UNION ALL 四个行为表，按用户ID过滤，用于计算用户-物品评分向量</p>
     *
     * @param userId 用户ID
     * @return 用户行为列表
     */
    List<UserBehaviorVO> selectUserBehaviors(@Param("userId") Integer userId);

    /**
     * 查询所有用户的所有有效行为记录
     * <p>用于定时任务全量重算物品相似度矩阵</p>
     *
     * @return 全量行为列表
     */
    List<UserBehaviorVO> selectAllBehaviors();
}
