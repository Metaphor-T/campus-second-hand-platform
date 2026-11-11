package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class AfterSaleApplyDTO {
    @NotBlank(message="请选择申请方式")
    @Pattern(regexp="REGULAR|ADMIN_INTERVENTION",message="申请方式无效")
    private String requestType;
    @NotBlank(message="请填写售后申请原因")
    @Size(max=1000,message="申请原因最多1000字")
    private String reason;
    @NotNull(message="请选择是否需要退回商品")
    private Boolean returnRequired;
    @Size(max=3,message="凭证图片最多3张")
    private List<@NotBlank(message="凭证图片地址无效") String> evidence;
}
