package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.service.InformationSystemService;
import com.eam.assetcenter.web.request.InformationSystemRelationRequest;
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
 * 信息系统控制器，提供系统主数据和关系维护接口。
 */
@Tag(name = "信息系统")
@RestController
@RequestMapping("/api/v1/information-systems")
@RequiredArgsConstructor
public class InformationSystemController {

    private final InformationSystemService informationSystemService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增信息系统")
    @PostMapping
    public ApiResponse<InformationSystem> create(@Validated @RequestBody InformationSystem informationSystem) {
        return ApiResponse.success(informationSystemService.create(informationSystem));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新信息系统")
    @PutMapping("/{id}")
    public ApiResponse<InformationSystem> update(@PathVariable Long id, @Validated @RequestBody InformationSystem informationSystem) {
        return ApiResponse.success(informationSystemService.update(id, informationSystem));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询信息系统详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(informationSystemService.getDetail(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询信息系统")
    @GetMapping
    public ApiResponse<PageResponse<InformationSystem>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String systemType,
                                                             @RequestParam(required = false) String status) {
        return ApiResponse.success(informationSystemService.page(pageNo, pageSize, keyword, systemType, status));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询信息系统下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<InformationSystem>> options() {
        return ApiResponse.success(informationSystemService.options());
    }

    /**
     * 同步资源的关联关系数据。
     */
    @Operation(summary = "同步信息系统关联关系")
    @PutMapping("/{id}/relations")
    public ApiResponse<Void> syncRelations(@PathVariable Long id, @RequestBody InformationSystemRelationRequest request) {
        informationSystemService.syncRelations(id, request);
        return ApiResponse.success("Relations synchronized", null);
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除信息系统")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        informationSystemService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


