package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 项目周期明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_period")
@Schema(description = "项目周期明细")
public class ProjectPeriod extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    /**
     * 项目主键。
     */
    @Schema(description = "项目主键")
    private Long projectId;

    /**
     * 阶段名称。
     */
    @Schema(description = "阶段名称")
    private String stageName;

    /**
     * 计划时间。
     */
    @Schema(description = "计划时间")
    private LocalDate plannedDate;

    /**
     * 实际时间。
     */
    @Schema(description = "实际时间")
    private LocalDate actualDate;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    private Integer sortOrder;
}
