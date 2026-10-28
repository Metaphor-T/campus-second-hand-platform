package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收货地址实体 - 对应数据库表 address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "address")
public class Address {

    /** 地址主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID（关联 base_user.id） */
    private Integer userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址（街道、门牌号等） */
    private String detailAddress;

    /** 邮政编码 */
    private String postalCode;

    /** 是否为默认地址：1-是，0-否 */
    private Integer isDefault;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
