package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.service.HardwareAssetService;
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
import java.util.List;
import java.util.Map;

/**
 * 硬件资产控制器，提供硬件台账、关系维护和生命周期接口。
 */
@Tag(name = "硬件资产")
@RestController
@RequestMapping("/api/v1/hardware-assets")
@RequiredArgsConstructor
public class HardwareAssetController {

    private final HardwareAssetService hardwareAssetService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增硬件资产")
    @PostMapping
    public ApiResponse<AssetHardware> create(@Validated @RequestBody HardwareAssetUpsertRequest request) {
        return ApiResponse.success(hardwareAssetService.create(request));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新硬件资产")
    @PutMapping("/{id}")
    public ApiResponse<AssetHardware> update(@PathVariable Long id, @Validated @RequestBody HardwareAssetUpsertRequest request) {
        return ApiResponse.success(hardwareAssetService.update(id, request));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询硬件资产详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(hardwareAssetService.getDetail(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询硬件资产")
    @GetMapping
    public ApiResponse<PageResponse<AssetHardware>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "10") int pageSize,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String hardwareCategory,
                                                         @RequestParam(required = false) String hardwareStatus,
                                                         @RequestParam(required = false) Long departmentId,
                                                         @RequestParam(required = false) Long locationId) {
        return ApiResponse.success(hardwareAssetService.page(pageNo, pageSize, keyword, hardwareCategory, hardwareStatus, departmentId, locationId));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询硬件资产下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<AssetHardware>> options() {
        return ApiResponse.success(hardwareAssetService.options());
    }

    /**
     * 同步硬件与信息系统之间的关联关系。
     */
    @Operation(summary = "同步硬件关联系统")
    @PutMapping("/{id}/systems")
    public ApiResponse<Void> syncSystems(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncSystems(id, request.getIds());
        return ApiResponse.success("Systems synchronized", null);
    }

    /**
     * 同步硬件负责人的关联关系。
     */
    @Operation(summary = "同步硬件负责人")
    @PutMapping("/{id}/owners")
    public ApiResponse<Void> syncOwners(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncOwners(id, request.getIds());
        return ApiResponse.success("Owners synchronized", null);
    }

    /**
     * 同步硬件与服务商之间的关联关系。
     */
    @Operation(summary = "同步硬件服务商")
    @PutMapping("/{id}/vendors")
    public ApiResponse<Void> syncVendors(@PathVariable Long id, @Validated @RequestBody IdListRequest request) {
        hardwareAssetService.syncVendors(id, request.getIds());
        return ApiResponse.success("Vendors synchronized", null);
    }

    /**
     * 执行硬件生命周期流转动作。
     */
    @Operation(summary = "执行硬件生命周期动作")
    @PostMapping("/{id}/lifecycle")
    public ApiResponse<AssetHardware> executeLifecycle(@PathVariable Long id, @Validated @RequestBody HardwareLifecycleRequest request) {
        return ApiResponse.success(hardwareAssetService.executeLifecycle(id, request));
    }

    /**
     * 批量导入硬件资产。
     */
    @Operation(summary = "批量导入硬件资产")
    @PostMapping("/import")
    public ApiResponse<List<AssetHardware>> batchImport(@Validated @RequestBody HardwareBatchImportRequest request) {
        return ApiResponse.success(hardwareAssetService.batchImport(request));
    }

    /**
     * 导出硬件资产列表文件。
     */
    @Operation(summary = "导出硬件资产")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] content = hardwareAssetService.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hardware-assets.csv")
                .contentType(new MediaType("text", "csv"))
                .body(content);
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除硬件资产")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hardwareAssetService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


