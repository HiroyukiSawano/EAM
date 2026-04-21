package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目资金支付周期明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_payment_cycle")
@Schema(description = "项目资金支付周期明细")
public class ProjectPaymentCycle extends BaseEntity {

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
     * 付款比例。
     */
    @Schema(description = "付款比例")
    private BigDecimal paymentRatio;

    /**
     * 付款金额。
     */
    @Schema(description = "付款金额")
    private BigDecimal paymentAmount;

    /**
     * 计划付款时间。
     */
    @Schema(description = "计划付款时间")
    private LocalDate plannedPaymentDate;

    /**
     * 实际付款时间。
     */
    @Schema(description = "实际付款时间")
    private LocalDate actualPaymentDate;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    private Integer sortOrder;
}
