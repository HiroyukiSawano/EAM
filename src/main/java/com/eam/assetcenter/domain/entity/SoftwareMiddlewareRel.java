package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 软件与中间件关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("software_middleware_rel")
public class SoftwareMiddlewareRel extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 软件资源主键，对应 information_system.id。
     */
    private Long softwareId;

    /**
     * 中间件资源主键。
     */
    private Long middlewareId;
}
