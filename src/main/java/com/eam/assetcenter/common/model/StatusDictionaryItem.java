package com.eam.assetcenter.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态字典项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "状态字典项")
public class StatusDictionaryItem {

    /**
     * 状态代码值。
     */
    @Schema(description = "状态代码值")
    private String value;

    /**
     * 中文标签。
     */
    @Schema(description = "中文标签")
    private String label;

    /**
     * 英文标签。
     */
    @Schema(description = "英文标签")
    private String labelEn;

    /**
     * 标签样式类型。
     */
    @Schema(description = "标签样式类型")
    private String tagType;
}
