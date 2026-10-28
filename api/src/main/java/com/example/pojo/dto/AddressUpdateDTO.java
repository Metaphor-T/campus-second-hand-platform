package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改收货地址参数
 */
@Data
public class AddressUpdateDTO {

    /** 地址主键ID */
    @NotNull(message = "地址ID不能为空")
    private Integer id;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "手机号不能为空")
    private String receiverPhone;

    /** 省份 */
    @NotBlank(message = "请选择省份")
    private String province;

    /** 城市 */
    @NotBlank(message = "请选择城市")
    private String city;

    /** 区/县 */
    @NotBlank(message = "请选择区/县")
    private String district;

    /** 详细地址（街道、门牌号等） */
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    /** 邮政编码 */
    private String postalCode;

    /** 是否为默认地址：1-是，0-否 */
    private Integer isDefault;
}
