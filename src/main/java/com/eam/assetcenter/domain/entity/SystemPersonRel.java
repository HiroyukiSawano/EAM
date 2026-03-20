package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信息系统与人员关联实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_person_rel")
public class SystemPersonRel extends BaseEntity {

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
     * 人员主键。
     */
    private Long personId;

    /**
     * 关联关系类型。
     */
    private String relationType;
}
