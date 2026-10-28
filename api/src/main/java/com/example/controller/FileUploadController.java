package com.example.controller;

import com.example.pojo.api.Result;
import com.example.pojo.vo.FileUploadResponseVO;
import com.example.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    public Result<FileUploadResponseVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subDir", required = false) String subDir) {
        try {
            // 上传成功，返回文件存储路径
            String filePath = fileUploadService.uploadFile(file, subDir);
            FileUploadResponseVO fileUploadResponseVO = FileUploadResponseVO.builder()
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .fileName(file.getOriginalFilename())
                    .build();
            return Result.success(fileUploadResponseVO);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

}