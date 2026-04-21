package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目资金支付周期请求对象。
 */
@Data
@Schema(description = "项目资金支付周期请求")
public class ProjectPaymentCycleRequest {

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "付款比例")
    private BigDecimal paymentRatio;

    @Schema(description = "付款金额")
    private BigDecimal paymentAmount;

    @Schema(description = "计划付款时间")
    private LocalDate plannedPaymentDate;

    @Schema(description = "实际付款时间")
    private LocalDate actualPaymentDate;
}
