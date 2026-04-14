package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 数据库资源新增或更新请求对象。
 */
@Data
@Schema(description = "数据库资源新增或更新请求")
public class DatabaseResourceUpsertRequest {

    @Schema(description = "数据库编码")
    @NotBlank(message = "databaseCode is required")
    private String databaseCode;

    @Schema(description = "数据库名称")
    @NotBlank(message = "databaseName is required")
    private String databaseName;

    @Schema(description = "数据库类型")
    @NotBlank(message = "databaseType is required")
    private String databaseType;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
