package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务商与人员关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_provider_person_rel")
public class ServiceProviderPersonRel extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 服务商主键。
     */
    private Long serviceProviderId;

    /**
     * 人员主键。
     */
    private Long personId;
}
