package com.example.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品审核参数
 */
@Data
public class ProductAuditDTO {

    /** 商品主键ID */
    @NotNull(message = "商品ID不能为空")
    private Integer id;

    /** 审核结果：1-通过，2-拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    /** 审核备注（拒绝时填写原因） */
    private String auditRemark;
}
