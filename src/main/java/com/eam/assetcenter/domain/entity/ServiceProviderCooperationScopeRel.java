package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务商合作范围关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_provider_cooperation_scope_rel")
public class ServiceProviderCooperationScopeRel extends BaseEntity {

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
     * 合作范围代码。
     */
    private String scopeCode;
}
