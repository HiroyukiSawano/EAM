package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 硬件与服务商关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_vendor_rel")
public class AssetHardwareVendorRel extends BaseEntity {

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
     * 服务商主键。
     */
    private Long serviceProviderId;
}
