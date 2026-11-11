package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AfterSaleReturnDTO {
    @NotBlank(message="请填写已收回商品的确认说明")
    @Size(max=1000,message="确认说明最多1000字")
    private String note;
}
