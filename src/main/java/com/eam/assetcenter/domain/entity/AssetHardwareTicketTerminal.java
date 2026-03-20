package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 取号机扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_ticket_terminal")
public class AssetHardwareTicketTerminal extends BaseEntity {

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
     * 打印机型号。
     */
    private String printerModel;

    /**
     * 是否支持二维码。
     */
    private Integer supportQr;

    /**
     * 设备型号。
     */
    private String deviceModel;
}
