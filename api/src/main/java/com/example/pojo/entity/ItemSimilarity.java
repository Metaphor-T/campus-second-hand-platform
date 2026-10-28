package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品相似度实体 - 对应数据库表 item_similarity
 * <p>存储预计算的基于物品的协同过滤余弦相似度</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "item_similarity")
public class ItemSimilarity {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 商品ID（较小值，用于唯一约束的去重配对） */
    private Integer productId1;

    /** 商品ID（较大值，用于唯一约束的去重配对） */
    private Integer productId2;

    /** 余弦相似度值（0~1，值越大表示两商品越相似） */
    private java.math.BigDecimal similarity;

    /** 相似度计算时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
