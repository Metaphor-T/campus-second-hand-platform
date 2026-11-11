package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AfterSaleResponseDTO {
    @NotNull(message="请选择同意或拒绝")
    private Boolean agree;
    @NotBlank(message="请填写处理说明；拒绝时需说明原因")
    @Size(max=1000,message="处理说明最多1000字")
    private String note;
}
