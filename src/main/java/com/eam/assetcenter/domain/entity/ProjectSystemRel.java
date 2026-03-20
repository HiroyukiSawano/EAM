package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目与信息系统关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_system_rel")
public class ProjectSystemRel extends BaseEntity {

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
     * 信息系统主键。
     */
    private Long informationSystemId;
}
