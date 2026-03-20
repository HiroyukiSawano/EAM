package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 硬件与人员关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_hardware_person_rel")
public class AssetHardwarePersonRel extends BaseEntity {

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
     * 人员主键。
     */
    private Long personId;

    /**
     * 关联关系类型。
     */
    private String relationType;
}
