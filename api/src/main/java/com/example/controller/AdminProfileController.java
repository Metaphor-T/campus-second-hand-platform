package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.dto.*;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.BaseUserVO;
import com.example.service.AdminProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/admin/profile") @Auth(RoleEnum.ADMIN) @RequiredArgsConstructor
public class AdminProfileController {
    private final AdminProfileService profiles;
    @PutMapping(consumes="application/json")
    public Result<BaseUserVO> save(@Valid @RequestBody AdminProfileUpdateDTO dto) { return Result.success(profiles.save(dto,null)); }
    @PutMapping(consumes="multipart/form-data")
    public Result<BaseUserVO> avatar(@Valid @RequestPart("profile") AdminProfileUpdateDTO dto,@RequestPart("avatar") MultipartFile avatar) { return Result.success(profiles.save(dto,avatar)); }
    @PutMapping("/password")
    public Result<Void> password(@Valid @RequestBody UserProfilePasswordDTO dto) { profiles.password(dto);return Result.success(); }
}
