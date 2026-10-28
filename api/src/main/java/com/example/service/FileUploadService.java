package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传服务类
 */
@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 单文件上传
     */
    public String uploadFile(MultipartFile file, String subDir) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");

        // 构建完整的上传路径
        StringBuilder fullPath = new StringBuilder(projectRoot)
                .append(File.separator)
                .append(uploadDir);

        if (subDir != null && !subDir.isEmpty()) {
            fullPath.append(File.separator).append(subDir);
        }

        // 创建目录
        File directory = new File(fullPath.toString());
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = Paths.get(fullPath.toString(), newFileName);
        Files.copy(file.getInputStream(), filePath);

        // 返回相对路径（用于前端访问）
        String relativePath = "/files/";
        if (subDir != null && !subDir.isEmpty()) {
            relativePath += subDir + "/";
        }
        relativePath += newFileName;

        return relativePath;
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径，格式如 /files/ab744dbd-b7e2-43d1-b9aa-177526918ad0.png
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        try {
            String projectRoot = System.getProperty("user.dir");

            // 处理不同格式的路径
            String relativePath = filePath;

            // 去掉开头的 /files/ 或 files/
            if (filePath.startsWith("/files/")) {
                relativePath = filePath.substring(7); // 去掉 "/files/"
            } else if (filePath.startsWith("files/")) {
                relativePath = filePath.substring(6); // 去掉 "files/"
            } else if (filePath.startsWith("/")) {
                relativePath = filePath.substring(1); // 去掉开头的 /
            }

            // 构建完整路径
            Path fullPath = Paths.get(projectRoot, uploadDir, relativePath);
            File file = fullPath.toFile();

            // 检查文件是否存在且可写
            if (file.exists() && file.isFile()) {
                // 确保文件在 uploadDir 目录内，防止路径遍历攻击
                Path canonicalPath = file.getCanonicalFile().toPath();
                Path uploadPath = Paths.get(projectRoot, uploadDir).toRealPath();

                if (!canonicalPath.startsWith(uploadPath)) {
                    // 路径遍历攻击，拒绝删除
                    return false;
                }

                return file.delete();
            }

            return false;
        } catch (Exception e) {
            log.error("删除文件失败: {}", filePath, e);
            return false;
        }
    }
}