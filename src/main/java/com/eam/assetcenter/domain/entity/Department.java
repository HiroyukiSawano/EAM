package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
@Schema(description = "部门")
public class Department extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 上级部门主键。
     */
    @Schema(description = "上级部门主键")
    private Long parentId;

    /**
     * 编码。
     */
    @Schema(description = "部门编码")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "部门名称")
    private String name;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;
}
