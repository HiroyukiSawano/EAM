package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务器扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_server")
public class AssetHardwareServer extends BaseEntity {

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
     * 操作系统。
     */
    private String operatingSystem;

    /**
     * 磁盘容量，单位 GB。
     */
    private Integer diskGb;

    /**
     * 虚拟化方式。
     */
    private String virtualization;
}
