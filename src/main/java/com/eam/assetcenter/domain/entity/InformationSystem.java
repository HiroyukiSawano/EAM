package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信息系统实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("information_system")
@Schema(description = "信息系统")
public class InformationSystem extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 编码。
     */
    @Schema(description = "系统编码")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "系统名称")
    private String name;

    /**
     * 系统类型。
     */
    @Schema(description = "系统类型")
    private String systemType;

    /**
     * 版本号。
     */
    @Schema(description = "版本号")
    private String versionNo;

    /**
     * 部署架构。
     */
    @Schema(description = "部署架构")
    private String deploymentArchitecture;

    /**
     * 负责人主键。
     */
    @Schema(description = "负责人主键")
    private Long ownerPersonId;

    /**
     * 联系电话。
     */
    @Schema(description = "联系电话")
    private String contactPhone;

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
