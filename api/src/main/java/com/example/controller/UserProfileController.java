package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.dto.UserProfilePasswordDTO;
import com.example.pojo.dto.UserProfileUpdateDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.UserProfileVO;
import com.example.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService profiles;
    @Auth(RoleEnum.USER) @GetMapping
    public Result<UserProfileVO> mine() { return Result.success(profiles.mine()); }
    @Auth(RoleEnum.USER) @PutMapping(consumes="application/json")
    public Result<UserProfileVO> save(@Valid @RequestBody UserProfileUpdateDTO dto) { return Result.success(profiles.save(dto,null)); }
    @Auth(RoleEnum.USER) @PutMapping(consumes="multipart/form-data")
    public Result<UserProfileVO> saveAvatar(@Valid @RequestPart("profile") UserProfileUpdateDTO dto,@RequestPart("avatar") MultipartFile avatar) { return Result.success(profiles.save(dto,avatar)); }
    @Auth(RoleEnum.USER) @PutMapping("/password")
    public Result<Void> password(@Valid @RequestBody UserProfilePasswordDTO dto) { profiles.changePassword(dto);return Result.success(); }
    @Auth(RoleEnum.ADMIN) @GetMapping("/admin/{id}")
    public Result<UserProfileVO> adminRead(@PathVariable Integer id) { return Result.success(profiles.forAdmin(id)); }
}
