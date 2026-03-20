package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLog extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源主键。
     */
    private Long resourceId;

    /**
     * 生命周期动作类型。
     */
    private String actionType;

    /**
     * 审计内容。
     */
    private String content;

    /**
     * 操作人。
     */
    private String operator;
}
