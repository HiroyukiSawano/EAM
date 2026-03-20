package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询机扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_query_terminal")
public class AssetHardwareQueryTerminal extends BaseEntity {

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
     * 屏幕尺寸。
     */
    private String screenSize;

    /**
     * 是否支持触摸。
     */
    private Integer touchEnabled;

    /**
     * 设备型号。
     */
    private String deviceModel;
}
