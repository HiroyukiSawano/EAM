package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.service.HardwareAssetService;
import com.eam.assetcenter.web.request.HardwareAssetRelationRequest;
import com.eam.assetcenter.web.request.HardwareAssetUpsertRequest;
import com.eam.assetcenter.web.request.HardwareBatchImportRequest;
import com.eam.assetcenter.web.request.HardwareLifecycleRequest;
import com.eam.assetcenter.web.request.IdListRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 硬件资产控制器，提供新版硬件台账、统计和关联维护接口。
 */
@Tag(name = "硬件资产")
@RestController
@RequestMapping("/api/v1/hardware-assets")
@RequiredArgsConstructor
public class HardwareAssetController {

    private final HardwareAssetService hardwareAssetService;

    @Operation(summary = "新增硬件资产")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody HardwareAssetUpsertRequest request) {
        return ApiResponse.success(hardwareAssetService.create(request));
    }

    @Operation(summary = "更新硬件资产")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody HardwareAssetUpsertRequest request) {
        return ApiResponse.success(hardwareAssetService.update(id, request));
    }

    @Operation(summary = "查询硬件统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(hardwareAssetService.stats());
    }

    @Operation(summary = "查询硬件资产详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(hardwareAssetService.getDetail(id));
    }

    @Operation(summary = "分页查询硬件资产")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "10") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String hardwareType,
                                                               @RequestParam(required = false) String hardwareCategory,
                                                               @RequestParam(required = false) String hardwareStatus,
                                                               @RequestParam(required = false) Long locationId) {
        return ApiResponse.success(hardwareAssetService.page(
                pageNo, pageSize, keyword, hardwareType, hardwareCategory, hardwareStatus, locationId));
    }

    @Operation(summary = "查询硬件资产下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<AssetHardware>> options() {
        return ApiResponse.success(hardwareAssetService.options());
    }

    @Operation(summary = "同步硬件关联关系")
    @PutMapping("/{id}/relations")
    public ApiResponse<Void> syncRelations(@PathVariable Long id, @RequestBody HardwareAssetRelationRequest request) {
        hardwareAssetService.syncRelations(id, request);
        return ApiResponse.success("Relations synchronized", null);
    }

    @Operation(summary = "同步硬件关联系统")
    @PutMapping("/{id}/systems")
    public ApiResponse<Void> syncSystems(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncSystems(id, request.getIds());
        return ApiResponse.success("Systems synchronized", null);
    }

    @Operation(summary = "同步硬件负责人")
    @PutMapping("/{id}/owners")
    public ApiResponse<Void> syncOwners(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncOwners(id, request.getIds());
        return ApiResponse.success("Owners synchronized", null);
    }

    @Operation(summary = "同步硬件服务商")
    @PutMapping("/{id}/vendors")
    public ApiResponse<Void> syncVendors(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncVendors(id, request.getIds());
        return ApiResponse.success("Vendors synchronized", null);
    }

    @Operation(summary = "执行硬件生命周期动作")
    @PostMapping("/{id}/lifecycle")
    public ApiResponse<Map<String, Object>> executeLifecycle(@PathVariable Long id, @Validated @RequestBody HardwareLifecycleRequest request) {
        return ApiResponse.success(hardwareAssetService.executeLifecycle(id, request));
    }

    @Operation(summary = "批量导入硬件资产")
    @PostMapping("/import")
    public ApiResponse<List<Map<String, Object>>> batchImport(@Validated @RequestBody HardwareBatchImportRequest request) {
        return ApiResponse.success(hardwareAssetService.batchImport(request));
    }

    @Operation(summary = "导出硬件资产")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] content = hardwareAssetService.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hardware-assets.csv")
                .contentType(new MediaType("text", "csv"))
                .body(content);
    }

    @Operation(summary = "删除硬件资产")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hardwareAssetService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}
