package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户行为视图对象 - 用于协同过滤推荐算法
 * <p>从收藏/想要/评价/下单四个表中 UNION ALL 查询的结果</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorVO {

    /** 用户ID */
    private Integer userId;

    /** 商品ID */
    private Integer productId;

    /**
     * 行为类型
     * <ul>
     *   <li>1 - 浏览（权重1）</li>
     *   <li>2 - 想要（权重3）</li>
     *   <li>3 - 收藏（权重5）</li>
     *   <li>4 - 评价（权重4 + 评分配置）</li>
     *   <li>5 - 下单（权重10）</li>
     * </ul>
     */
    private Integer behaviorType;

    /**
     * 行为分值
     * <p>对于评价行为，该值为评分(1~5)；其他行为固定为1</p>
     */
    private Integer score;
}
