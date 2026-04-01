package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用文件控制器，提供图片上传与访问能力。
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传图片并返回访问地址。
     */
    @Operation(summary = "上传图片")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileStorageService.uploadImage(file));
    }

    /**
     * 读取指定图片文件。
     */
    @Operation(summary = "读取图片")
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadImage(fileName);
        return ResponseEntity.ok()
                .contentType(fileStorageService.resolveMediaType(fileName))
                .body(resource);
    }

    /**
     * 上传项目文档等通用文件。
     */
    @Operation(summary = "上传通用文件")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileStorageService.uploadDocument(file));
    }

    /**
     * 读取指定通用文件。
     */
    @Operation(summary = "读取通用文件")
    @GetMapping("/documents/{fileName:.+}")
    public ResponseEntity<Resource> getDocument(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadDocument(fileName);
        return ResponseEntity.ok()
                .contentType(fileStorageService.resolveMediaType(fileName))
                .body(resource);
    }
}
