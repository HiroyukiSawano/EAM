package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.AssetLocation;
import com.eam.assetcenter.service.AssetLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

/**
 * 资产位置控制器，提供位置主数据接口。
 */
@Tag(name = "资产位置")
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class AssetLocationController {

    private final AssetLocationService assetLocationService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增资产位置")
    @PostMapping
    public ApiResponse<AssetLocation> create(@Validated @RequestBody AssetLocation assetLocation) {
        return ApiResponse.success(assetLocationService.create(assetLocation));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新资产位置")
    @PutMapping("/{id}")
    public ApiResponse<AssetLocation> update(@PathVariable Long id, @Validated @RequestBody AssetLocation assetLocation) {
        return ApiResponse.success(assetLocationService.update(id, assetLocation));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询资产位置详情")
    @GetMapping("/{id}")
    public ApiResponse<AssetLocation> get(@PathVariable Long id) {
        return ApiResponse.success(assetLocationService.getById(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询资产位置")
    @GetMapping
    public ApiResponse<PageResponse<AssetLocation>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                         @RequestParam(defaultValue = "10") int pageSize,
                                                         @RequestParam(required = false) String keyword) {
        return ApiResponse.success(assetLocationService.page(pageNo, pageSize, keyword));
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除资产位置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetLocationService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


