package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目与服务商关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_vendor_rel")
public class ProjectVendorRel extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目主键。
     */
    private Long projectId;

    /**
     * 服务商主键。
     */
    private Long serviceProviderId;
}
