package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 项目周期请求对象。
 */
@Data
@Schema(description = "项目周期请求")
public class ProjectPeriodRequest {

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "计划时间")
    private LocalDate plannedDate;

    @Schema(description = "实际时间")
    private LocalDate actualDate;
}
