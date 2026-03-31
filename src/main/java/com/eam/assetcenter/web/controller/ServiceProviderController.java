package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.service.ServiceProviderService;
import com.eam.assetcenter.web.request.ServiceProviderRelationRequest;
import com.eam.assetcenter.web.request.ServiceProviderUpsertRequest;
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

import java.util.Map;

/**
 * 服务商控制器，提供服务商主数据接口。
 */
@Tag(name = "服务商管理")
@RestController
@RequestMapping("/api/v1/service-providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增服务商")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody ServiceProviderUpsertRequest request) {
        return ApiResponse.success(serviceProviderService.create(request));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新服务商")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody ServiceProviderUpsertRequest request) {
        return ApiResponse.success(serviceProviderService.update(id, request));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询服务商详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(serviceProviderService.getDetail(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询服务商")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "10") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String cooperationScope,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.success(serviceProviderService.page(pageNo, pageSize, keyword, cooperationScope, status));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询服务商下拉选项")
    @GetMapping("/options")
    public ApiResponse<java.util.List<Map<String, Object>>> options() {
        return ApiResponse.success(serviceProviderService.options());
    }

    /**
     * 查询服务商统计。
     */
    @Operation(summary = "查询服务商统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(serviceProviderService.stats());
    }

    /**
     * 同步服务商的关联关系数据。
     */
    @Operation(summary = "同步服务商关联关系")
    @PutMapping("/{id}/relations")
    public ApiResponse<Void> syncRelations(@PathVariable Long id, @Validated @RequestBody ServiceProviderRelationRequest request) {
        serviceProviderService.syncRelations(id, request);
        return ApiResponse.success("Relations synchronized", null);
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除服务商")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceProviderService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


