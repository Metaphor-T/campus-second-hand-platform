package com.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 文件上传配置类
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // 获取项目根目录（src的父目录）
        String projectRoot = System.getProperty("user.dir");
        String fullUploadPath = projectRoot + File.separator + uploadDir;

        File uploadDirectory = new File(fullUploadPath);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径，使上传的文件可以通过URL访问
        String projectRoot = System.getProperty("user.dir");
        String fullUploadPath = "file:" + projectRoot + File.separator + uploadDir + File.separator;

        registry.addResourceHandler("/files/**")
                .addResourceLocations(fullUploadPath);
    }
}