package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 中间件资源新增或更新请求对象。
 */
@Data
@Schema(description = "中间件资源新增或更新请求")
public class MiddlewareResourceUpsertRequest {

    @Schema(description = "中间件编码")
    @NotBlank(message = "middlewareCode is required")
    private String middlewareCode;

    @Schema(description = "中间件名称")
    @NotBlank(message = "middlewareName is required")
    private String middlewareName;

    @Schema(description = "中间件类型")
    @NotBlank(message = "middlewareType is required")
    private String middlewareType;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
