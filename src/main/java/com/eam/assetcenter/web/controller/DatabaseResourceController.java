package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.DatabaseResource;
import com.eam.assetcenter.service.DatabaseResourceService;
import com.eam.assetcenter.web.request.DatabaseResourceUpsertRequest;
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
 * 数据库资源控制器。
 */
@Tag(name = "数据库资源")
@RestController
@RequestMapping("/api/v1/database-resources")
@RequiredArgsConstructor
public class DatabaseResourceController {

    private final DatabaseResourceService databaseResourceService;

    @Operation(summary = "新增数据库资源")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody DatabaseResourceUpsertRequest request) {
        return ApiResponse.success(databaseResourceService.create(request));
    }

    @Operation(summary = "更新数据库资源")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody DatabaseResourceUpsertRequest request) {
        return ApiResponse.success(databaseResourceService.update(id, request));
    }

    @Operation(summary = "查询数据库统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(databaseResourceService.stats());
    }

    @Operation(summary = "查询数据库资源详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(databaseResourceService.getDetail(id));
    }

    @Operation(summary = "分页查询数据库资源")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "10") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String databaseType,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.success(databaseResourceService.page(pageNo, pageSize, keyword, databaseType, status));
    }

    @Operation(summary = "查询数据库资源下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<DatabaseResource>> options() {
        return ApiResponse.success(databaseResourceService.options());
    }

    @Operation(summary = "删除数据库资源")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        databaseResourceService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}
