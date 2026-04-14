package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.MiddlewareResource;
import com.eam.assetcenter.service.MiddlewareResourceService;
import com.eam.assetcenter.web.request.MiddlewareResourceUpsertRequest;
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

import java.util.List;
import java.util.Map;

/**
 * 中间件资源控制器。
 */
@Tag(name = "中间件资源")
@RestController
@RequestMapping("/api/v1/middleware-resources")
@RequiredArgsConstructor
public class MiddlewareResourceController {

    private final MiddlewareResourceService middlewareResourceService;

    @Operation(summary = "新增中间件资源")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody MiddlewareResourceUpsertRequest request) {
        return ApiResponse.success(middlewareResourceService.create(request));
    }

    @Operation(summary = "更新中间件资源")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody MiddlewareResourceUpsertRequest request) {
        return ApiResponse.success(middlewareResourceService.update(id, request));
    }

    @Operation(summary = "查询中间件统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(middlewareResourceService.stats());
    }

    @Operation(summary = "查询中间件资源详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(middlewareResourceService.getDetail(id));
    }

    @Operation(summary = "分页查询中间件资源")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "10") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String middlewareType,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.success(middlewareResourceService.page(pageNo, pageSize, keyword, middlewareType, status));
    }

    @Operation(summary = "查询中间件资源下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<MiddlewareResource>> options() {
        return ApiResponse.success(middlewareResourceService.options());
    }

    @Operation(summary = "删除中间件资源")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        middlewareResourceService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}
