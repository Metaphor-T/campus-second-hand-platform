package com.example.pojo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CampusVerificationApplyDTO(
    @NotNull(message="请先同意校园认证资料使用说明")
    @AssertTrue(message="请先同意校园认证资料使用说明") Boolean consent) {}
