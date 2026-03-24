package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.service.PersonService;
import com.eam.assetcenter.web.request.PersonRelationRequest;
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
 * 人员控制器，提供人员主数据接口。
 */
@Tag(name = "人员管理")
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /**
     * 新增资源记录。
     */
    @Operation(summary = "新增人员")
    @PostMapping
    public ApiResponse<Person> create(@Validated @RequestBody Person person) {
        return ApiResponse.success(personService.create(person));
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Operation(summary = "更新人员")
    @PutMapping("/{id}")
    public ApiResponse<Person> update(@PathVariable Long id, @Validated @RequestBody Person person) {
        return ApiResponse.success(personService.update(id, person));
    }

    /**
     * 查询指定主键对应的资源详情。
     */
    @Operation(summary = "查询人员详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(personService.getDetail(id));
    }

    /**
     * 按条件分页查询资源列表。
     */
    @Operation(summary = "分页查询人员")
    @GetMapping
    public ApiResponse<PageResponse<Person>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long departmentId,
                                                  @RequestParam(required = false) String status) {
        return ApiResponse.success(personService.page(pageNo, pageSize, keyword, departmentId, status));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    @Operation(summary = "查询人员下拉选项")
    @GetMapping("/options")
    public ApiResponse<List<Person>> options() {
        return ApiResponse.success(personService.options());
    }

    /**
     * 同步人员的关联关系数据。
     */
    @Operation(summary = "同步人员关联关系")
    @PutMapping("/{id}/relations")
    public ApiResponse<Void> syncRelations(@PathVariable Long id, @Validated @RequestBody PersonRelationRequest request) {
        personService.syncRelations(id, request);
        return ApiResponse.success("Relations synchronized", null);
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    @Operation(summary = "删除人员")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ApiResponse.success("Deleted", null);
    }
}


