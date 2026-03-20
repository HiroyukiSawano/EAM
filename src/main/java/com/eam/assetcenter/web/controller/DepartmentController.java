package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.service.DepartmentService;
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

/**
 * 部门控制器，提供部门主数据接口。
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增部门")
    @PostMapping
    public ApiResponse<Department> create(@Validated @RequestBody Department department) {
        return ApiResponse.success(departmentService.create(department));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable Long id, @Validated @RequestBody Department department) {
        return ApiResponse.success(departmentService.update(id, department));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询部门详情")
    @GetMapping("/{id}")
    public ApiResponse<Department> get(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询部门")
    @GetMapping
    public ApiResponse<PageResponse<Department>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.success(departmentService.page(pageNo, pageSize, keyword));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询部门下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<Department>> options() {
        return ApiResponse.success(departmentService.listAll());
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


