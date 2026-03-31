package com.eam.assetcenter.service;

import com.eam.assetcenter.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 通用图片文件服务，负责本地上传与读取。
 */
@Service
@RequiredArgsConstructor
public class ImageFileService {

    private static final Set<String> ALLOWED_EXTENSIONS = new LinkedHashSet<String>(Arrays.asList(
            "png", "jpg", "jpeg", "webp", "svg"
    ));

    private static final Set<String> ALLOWED_CONTENT_TYPES = new LinkedHashSet<String>(Arrays.asList(
            "image/png", "image/jpeg", "image/webp", "image/svg+xml"
    ));

    @Value("${asset-center.file-storage.base-dir}")
    private String baseDir;

    @Value("${asset-center.file-storage.public-base-path}")
    private String publicBasePath;

    @Value("${asset-center.file-storage.max-image-size-bytes}")
    private long maxImageSizeBytes;

    /**
     * 上传图片并返回访问地址。
     */
    public Map<String, Object> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new BusinessException("图片大小不能超过 2MB");
        }

        String extension = resolveExtension(file);
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 png、jpg、jpeg、webp、svg 图片上传");
        }

        Path storageDir = resolveStorageDir();
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = storageDir.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(storageDir);
            InputStream inputStream = file.getInputStream();
            try {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                inputStream.close();
            }
        } catch (IOException exception) {
            throw new BusinessException("图片上传失败: " + exception.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("url", buildPublicUrl(storedFileName));
        result.put("originalName", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("contentType", contentType);
        return result;
    }

    /**
     * 读取指定图片资源。
     */
    public Resource loadImage(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException("图片路径不合法");
        }

        Path targetPath = resolveStorageDir().resolve(fileName).normalize();
        if (!targetPath.startsWith(resolveStorageDir()) || !Files.exists(targetPath)) {
            throw new BusinessException("图片不存在");
        }

        try {
            return new UrlResource(targetPath.toUri());
        } catch (MalformedURLException exception) {
            throw new BusinessException("图片读取失败: " + exception.getMessage());
        }
    }

    /**
     * 推断图片响应类型。
     */
    public MediaType resolveMediaType(String fileName) {
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Path resolveStorageDir() {
        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    private String buildPublicUrl(String storedFileName) {
        String prefix = StringUtils.trimTrailingCharacter(publicBasePath, '/');
        return prefix + "/" + storedFileName;
    }

    private String resolveExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension)) {
            return extension.trim().toLowerCase();
        }

        String contentType = normalizeContentType(file.getContentType());
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        if ("image/svg+xml".equals(contentType)) {
            return "svg";
        }
        throw new BusinessException("无法识别图片格式");
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase();
    }
}
