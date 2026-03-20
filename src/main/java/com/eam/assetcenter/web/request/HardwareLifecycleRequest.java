package com.eam.assetcenter.web.request;

import com.eam.assetcenter.common.enums.LifecycleActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 硬件生命周期流转请求对象。
 */
@Data
@Schema(description = "硬件生命周期流转请求")
public class HardwareLifecycleRequest {

    /**
     * 生命周期动作。
     */
    @Schema(description = "生命周期动作")
    @NotNull(message = "action is required")
    private LifecycleActionType action;

    /**
     * 原因说明。
     */
    @Schema(description = "原因说明")
    private String reason;

    /**
     * 操作人。
     */
    @Schema(description = "操作人")
    private String operator;
}
