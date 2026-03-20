package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 硬件资产主实体，对应硬件公共台账信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware")
@Schema(description = "硬件资产")
public class AssetHardware extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 资产编码。
     */
    @Schema(description = "资产编码")
    private String assetCode;

    /**
     * 资产名称。
     */
    @Schema(description = "资产名称")
    private String assetName;

    /**
     * 硬件分类。
     */
    @Schema(description = "硬件分类")
    private String hardwareCategory;

    /**
     * 位置主键。
     */
    @Schema(description = "位置主键")
    private Long locationId;

    /**
     * 所属部门主键。
     */
    @Schema(description = "所属部门主键")
    private Long departmentId;

    /**
     * 管理 IP 地址。
     */
    @Schema(description = "管理 IP 地址")
    private String managementIp;

    /**
     * 业务 IP 地址。
     */
    @Schema(description = "业务 IP 地址")
    private String businessIp;

    /**
     * CPU 型号。
     */
    @Schema(description = "CPU 型号")
    private String cpuModel;

    /**
     * CPU 核心数。
     */
    @Schema(description = "CPU 核心数")
    private Integer cpuCores;

    /**
     * 内存容量，单位 GB。
     */
    @Schema(description = "内存容量，单位 GB")
    private Integer memoryGb;

    /**
     * 硬件状态。
     */
    @Schema(description = "硬件状态")
    private String hardwareStatus;

    /**
     * 启用日期。
     */
    @Schema(description = "启用日期")
    private LocalDate enabledDate;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;
}
