package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 硬件与信息系统关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_system_rel")
public class AssetHardwareSystemRel extends BaseEntity {

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
     * 信息系统主键。
     */
    private Long informationSystemId;
}
