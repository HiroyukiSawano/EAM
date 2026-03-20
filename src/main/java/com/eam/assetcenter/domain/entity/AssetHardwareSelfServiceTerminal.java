package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自助终端扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_self_service_terminal")
public class AssetHardwareSelfServiceTerminal extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 硬件资产主键。
     */
    private Long hardwareAssetId;

    /**
     * 自助终端类型。
     */
    private String terminalType;

    /**
     * 屏幕尺寸。
     */
    private String screenSize;

    /**
     * 设备型号。
     */
    private String deviceModel;
}
