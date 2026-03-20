package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信息系统与服务商关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_vendor_rel")
public class SystemVendorRel extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 信息系统主键。
     */
    private Long informationSystemId;

    /**
     * 服务商主键。
     */
    private Long serviceProviderId;
}
