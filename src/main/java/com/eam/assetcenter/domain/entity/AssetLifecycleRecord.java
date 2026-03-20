package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 硬件生命周期记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_lifecycle_record")
public class AssetLifecycleRecord extends BaseEntity {

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
     * 生命周期动作类型。
     */
    private String actionType;

    /**
     * 变更前状态。
     */
    private String fromStatus;

    /**
     * 变更后状态。
     */
    private String toStatus;

    /**
     * 原因说明。
     */
    private String reason;

    /**
     * 操作人。
     */
    private String operator;

    /**
     * 动作发生时间。
     */
    private LocalDateTime actionTime;
}
