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
 * 本地文件存储服务，统一处理图片与文档上传。
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = new LinkedHashSet<String>(Arrays.asList(
            "png", "jpg", "jpeg", "webp", "svg"
    ));

    private static final Set<String> IMAGE_CONTENT_TYPES = new LinkedHashSet<String>(Arrays.asList(
            "image/png", "image/jpeg", "image/webp", "image/svg+xml"
    ));

    private static final Set<String> DOCUMENT_EXTENSIONS = new LinkedHashSet<String>(Arrays.asList(
            "png", "jpg", "jpeg", "pdf", "doc", "docx", "xls", "xlsx"
    ));

    private static final Set<String> DOCUMENT_CONTENT_TYPES = new LinkedHashSet<String>(Arrays.asList(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ));

    @Value("${asset-center.file-storage.base-dir}")
    private String baseDir;

    @Value("${asset-center.file-storage.image-public-base-path}")
    private String imagePublicBasePath;

    @Value("${asset-center.file-storage.document-public-base-path}")
    private String documentPublicBasePath;

    @Value("${asset-center.file-storage.max-image-size-bytes}")
    private long maxImageSizeBytes;

    @Value("${asset-center.file-storage.max-document-size-bytes}")
    private long maxDocumentSizeBytes;

    /**
     * 上传图片。
     */
    public Map<String, Object> uploadImage(MultipartFile file) {
        return upload(file, "images", imagePublicBasePath, IMAGE_EXTENSIONS, IMAGE_CONTENT_TYPES, maxImageSizeBytes,
                "图片大小不能超过 2MB", "仅支持 png、jpg、jpeg、webp、svg 图片上传");
    }

    /**
     * 上传通用文档。
     */
    public Map<String, Object> uploadDocument(MultipartFile file) {
        return upload(file, "documents", documentPublicBasePath, DOCUMENT_EXTENSIONS, DOCUMENT_CONTENT_TYPES, maxDocumentSizeBytes,
                "文件大小不能超过 100MB", "仅支持 png、jpg、jpeg、pdf、word、excel 文件上传");
    }

    /**
     * 读取图片资源。
     */
    public Resource loadImage(String fileName) {
        return loadResource("images", fileName);
    }

    /**
     * 读取文档资源。
     */
    public Resource loadDocument(String fileName) {
        return loadResource("documents", fileName);
    }

    /**
     * 推断响应类型。
     */
    public MediaType resolveMediaType(String fileName) {
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Map<String, Object> upload(MultipartFile file,
                                       String subDir,
                                       String publicBasePath,
                                       Set<String> allowedExtensions,
                                       Set<String> allowedContentTypes,
                                       long maxSizeBytes,
                                       String sizeErrorMessage,
                                       String typeErrorMessage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(sizeErrorMessage);
        }

        String extension = resolveExtension(file);
        String contentType = normalizeContentType(file.getContentType());
        if (!allowedExtensions.contains(extension) || !allowedContentTypes.contains(contentType)) {
            throw new BusinessException(typeErrorMessage);
        }

        Path storageDir = resolveStorageDir(subDir);
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
            throw new BusinessException("文件上传失败: " + exception.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("fileName", storedFileName);
        result.put("url", buildPublicUrl(publicBasePath, storedFileName));
        result.put("originalName", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("contentType", contentType);
        return result;
    }

    private Resource loadResource(String subDir, String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException("文件路径不合法");
        }

        Path storageDir = resolveStorageDir(subDir);
        Path targetPath = storageDir.resolve(fileName).normalize();
        if (!targetPath.startsWith(storageDir) || !Files.exists(targetPath)) {
            throw new BusinessException("文件不存在");
        }

        try {
            return new UrlResource(targetPath.toUri());
        } catch (MalformedURLException exception) {
            throw new BusinessException("文件读取失败: " + exception.getMessage());
        }
    }

    private Path resolveStorageDir(String subDir) {
        return Paths.get(baseDir, subDir).toAbsolutePath().normalize();
    }

    private String buildPublicUrl(String publicBasePath, String storedFileName) {
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
        if ("application/pdf".equals(contentType)) {
            return "pdf";
        }
        if ("application/msword".equals(contentType)) {
            return "doc";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return "docx";
        }
        if ("application/vnd.ms-excel".equals(contentType)) {
            return "xls";
        }
        if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) {
            return "xlsx";
        }
        throw new BusinessException("无法识别文件格式");
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase();
    }
}
