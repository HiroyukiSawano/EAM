package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目与人员关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_person_rel")
public class ProjectPersonRel extends BaseEntity {

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
     * 人员主键。
     */
    private Long personId;

    /**
     * 关联关系类型。
     */
    private String relationType;
}
