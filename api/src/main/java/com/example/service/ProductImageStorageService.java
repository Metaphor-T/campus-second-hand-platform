package com.example.service;

import com.example.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** 商品图片专用存储，不改变头像、聊天图片等其他成员的上传接口。 */
@Service
@Slf4j
public class ProductImageStorageService {
    public static final long MAX_BYTES = 2L * 1024 * 1024;
    private final Path root;
    public ProductImageStorageService(@Value("${file.upload-dir}") String directory) {
        Path configured = Path.of(directory);
        root = (configured.isAbsolute() ? configured : Path.of(System.getProperty("user.dir")).resolve(configured)).toAbsolutePath().normalize();
    }
    public String store(Integer userId, MultipartFile file) {
        if (userId == null || userId <= 0) throw new BusinessException("请先登录再上传商品图片", 400);
        if (file == null || file.isEmpty()) throw new BusinessException("请选择有效图片", 400);
        if (file.getSize() > MAX_BYTES) throw new BusinessException("单张图片不能超过2MB", 400);
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!name.matches(".*\\.(png|jpe?g)$")) throw new BusinessException("仅支持JPG/PNG图片", 400);
        if (!Set.of("image/png", "image/jpeg", "image/jpg").contains(Optional.ofNullable(file.getContentType()).orElse(""))) {
            throw new BusinessException("图片类型无效，请选择JPG/PNG", 400);
        }
        Path written = null;
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = input == null ? Collections.emptyIterator() : ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException("文件内容不是有效图片", 400);
            ImageReader reader = readers.next();
            BufferedImage image;
            String format;
            try {
                reader.setInput(input, true, true);
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg").contains(format)) throw new BusinessException("文件内容必须是JPG/PNG图片", 400);
                boolean png = format.equals("png");
                if (png != name.endsWith(".png")) throw new BusinessException("图片扩展名与实际内容不一致", 400);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096 || (long) width * height > 16000000L) {
                    throw new BusinessException("图片边长最多4096像素、总像素最多1600万，请缩小后上传", 400);
                }
                image = reader.read(0);
                if (image == null) throw new BusinessException("图片已损坏，请重新选择", 400);
            } finally { reader.dispose(); }
            Path directory = root.resolve("products").resolve(userId.toString()).normalize();
            Files.createDirectories(directory);
            if (!directory.toRealPath().startsWith(root.toRealPath())) throw new BusinessException("图片存储目录无效", 400);
            String filename = UUID.randomUUID() + (format.equals("png") ? ".png" : ".jpg");
            written = directory.resolve(filename);
            if (!ImageIO.write(image, format.equals("png") ? "png" : "jpeg", written.toFile())) {
                throw new BusinessException("图片转换失败，请重新选择", 400);
            }
            return "/files/products/" + userId + "/" + filename;
        } catch (BusinessException e) {
            cleanup(written);
            throw e;
        } catch (IOException | RuntimeException e) {
            cleanup(written);
            log.warn("商品图片写入失败", e);
            throw new BusinessException("图片保存失败，请稍后重试", 500);
        }
    }
    private void cleanup(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException e) { log.warn("清理未完成图片失败", e); }
    }
}
