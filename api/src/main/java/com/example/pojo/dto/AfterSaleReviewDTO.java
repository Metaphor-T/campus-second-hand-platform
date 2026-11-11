package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AfterSaleReviewDTO {
    @NotNull(message="请选择审核结果")
    private Boolean approved;
    @NotBlank(message="请填写审核说明")
    @Size(max=1000,message="审核说明最多1000字")
    private String note;
    private Boolean returnRequired;
    /** 仅用于核对缺少原交易状态的历史售后，不能用于修改新申请的状态。 */
    @Min(value=2,message="历史交易状态只能选择待收货或已完成")
    @Max(value=3,message="历史交易状态只能选择待收货或已完成")
    private Integer originalOrderStatus;
}
