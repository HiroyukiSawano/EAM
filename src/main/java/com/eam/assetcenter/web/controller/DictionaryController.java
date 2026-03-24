package com.eam.assetcenter.web.controller;

import com.eam.assetcenter.common.api.ApiResponse;
import com.eam.assetcenter.common.model.StatusDictionaryItem;
import com.eam.assetcenter.service.StatusDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 字典控制器，提供前端所需的静态字典数据。
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final StatusDictionaryService statusDictionaryService;

    /**
     * 查询全部状态字典。
     */
    @Operation(summary = "查询状态字典")
    @GetMapping("/statuses")
    public ApiResponse<Map<String, List<StatusDictionaryItem>>> statuses() {
        return ApiResponse.success(statusDictionaryService.listStatusDictionaries());
    }
}
