package com.eam.assetcenter.web.request;

import com.eam.assetcenter.common.enums.HardwareCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 硬件资产新增或更新请求对象。
 */
@Data
@Schema(description = "硬件资产新增或更新请求")
public class HardwareAssetUpsertRequest {

    /**
     * 资产编码。
     */
    @Schema(description = "资产编码")
    @NotBlank(message = "assetCode is required")
    private String assetCode;

    /**
     * 资产名称。
     */
    @Schema(description = "资产名称")
    @NotBlank(message = "assetName is required")
    private String assetName;

    /**
     * 硬件分类。
     */
    @Schema(description = "硬件分类")
    @NotNull(message = "hardwareCategory is required")
    private HardwareCategory hardwareCategory;

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
     * 启用日期。
     */
    @Schema(description = "启用日期")
    private LocalDate enabledDate;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 操作系统。
     */
    @Schema(description = "操作系统")
    private String operatingSystem;

    /**
     * 磁盘容量，单位 GB。
     */
    @Schema(description = "磁盘容量，单位 GB")
    private Integer diskGb;

    /**
     * 虚拟化方式。
     */
    @Schema(description = "虚拟化方式")
    private String virtualization;

    /**
     * 屏幕尺寸。
     */
    @Schema(description = "屏幕尺寸")
    private String screenSize;

    /**
     * 是否支持触摸。
     */
    @Schema(description = "是否支持触摸")
    private Boolean touchEnabled;

    /**
     * 设备型号。
     */
    @Schema(description = "设备型号")
    private String deviceModel;

    /**
     * 打印机型号。
     */
    @Schema(description = "打印机型号")
    private String printerModel;

    /**
     * 是否支持二维码。
     */
    @Schema(description = "是否支持二维码")
    private Boolean supportQr;

    /**
     * 自助终端类型。
     */
    @Schema(description = "自助终端类型")
    private String terminalType;
}
