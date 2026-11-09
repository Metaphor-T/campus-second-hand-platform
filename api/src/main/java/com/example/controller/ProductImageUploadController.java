package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.FileUploadResponseVO;
import com.example.service.ProductImageStorageService;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/product/images")
@RequiredArgsConstructor
public class ProductImageUploadController {
    private final ProductImageStorageService storage;
    @Auth({RoleEnum.USER})
    @PostMapping("/upload")
    public Result<FileUploadResponseVO> upload(@RequestParam("file") MultipartFile file) {
        String path = storage.store(UserContext.getUserId(), file);
        return Result.success(FileUploadResponseVO.builder().filePath(path).fileName(file.getOriginalFilename()).fileSize(file.getSize()).build());
    }
}
