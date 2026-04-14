package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 中间件资源实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("middleware_resource")
@Schema(description = "中间件资源")
public class MiddlewareResource extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 中间件编码。
     */
    @Schema(description = "中间件编码")
    private String middlewareCode;

    /**
     * 中间件名称。
     */
    @Schema(description = "中间件名称")
    private String middlewareName;

    /**
     * 中间件类型。
     */
    @Schema(description = "中间件类型")
    private String middlewareType;

    /**
     * 版本号。
     */
    @Schema(description = "版本号")
    private String version;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;
}
