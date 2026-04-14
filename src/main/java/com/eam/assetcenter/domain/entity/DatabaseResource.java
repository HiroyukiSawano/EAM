package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据库资源实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("database_resource")
@Schema(description = "数据库资源")
public class DatabaseResource extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 数据库编码。
     */
    @Schema(description = "数据库编码")
    private String databaseCode;

    /**
     * 数据库名称。
     */
    @Schema(description = "数据库名称")
    private String databaseName;

    /**
     * 数据库类型。
     */
    @Schema(description = "数据库类型")
    private String databaseType;

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
