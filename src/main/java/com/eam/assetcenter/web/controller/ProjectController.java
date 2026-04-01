package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.service.ProjectService;
import com.eam.assetcenter.web.request.ProjectRelationRequest;
import com.eam.assetcenter.web.request.ProjectUpsertRequest;
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
 * 项目控制器，提供项目主数据和关系维护接口。
 */
@Tag(name = "项目管理")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增项目")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody ProjectUpsertRequest request) {
        return ApiResponse.success(projectService.create(request));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody ProjectUpsertRequest request) {
        return ApiResponse.success(projectService.update(id, request));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询项目详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(projectService.getDetail(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询项目")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "10") int pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String projectType,
                                                               @RequestParam(required = false) String projectStatus) {
        return ApiResponse.success(projectService.page(pageNo, pageSize, keyword, projectType, projectStatus));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询项目下拉选项")
    @GetMapping("/options")
    public ApiResponse<java.util.List<com.eam.assetcenter.domain.entity.ProjectInfo>> options() {
        return ApiResponse.success(projectService.options());
    }

    /**
     * 查询项目统计。
     */
    @Operation(summary = "查询项目统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(projectService.stats());
    }

    /**
     * 同步资源的关联关系数据。
     */
    @Operation(summary = "同步项目关联关系")
    @PutMapping("/{id}/relations")
    public ApiResponse<Void> syncRelations(@PathVariable Long id, @RequestBody ProjectRelationRequest request) {
        projectService.syncRelations(id, request);
        return ApiResponse.success("Relations synchronized", null);
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


