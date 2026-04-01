package com.eam.assetcenter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片文件服务兼容层，复用通用文件存储实现。
 */
@Service
@RequiredArgsConstructor
public class ImageFileService {

    private final FileStorageService fileStorageService;

    /**
     * 上传图片并返回访问地址。
     */
    public Map<String, Object> uploadImage(MultipartFile file) {
        return fileStorageService.uploadImage(file);
    }

    /**
     * 读取指定图片资源。
     */
    public Resource loadImage(String fileName) {
        return fileStorageService.loadImage(fileName);
    }

    /**
     * 推断图片响应类型。
     */
    public MediaType resolveMediaType(String fileName) {
        return fileStorageService.resolveMediaType(fileName);
    }
}
