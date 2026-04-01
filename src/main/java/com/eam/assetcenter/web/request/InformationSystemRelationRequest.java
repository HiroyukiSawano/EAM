package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 信息系统关联关系维护请求对象。
 */
@Data
@Schema(description = "信息系统关联关系请求")
public class InformationSystemRelationRequest {

    /**
     * 服务商主键列表。
     */
    @Schema(description = "服务商主键列表")
    private List<Long> serviceProviderIds;

    /**
     * 人员主键列表。
     */
    @Schema(description = "人员主键列表")
    private List<Long> personIds;

    /**
     * 硬件主键列表。
     */
    @Schema(description = "硬件主键列表")
    private List<Long> hardwareAssetIds;
}
