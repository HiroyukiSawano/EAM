package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 硬件资产关联关系维护请求对象。
 */
@Data
@Schema(description = "硬件资产关联关系请求")
public class HardwareAssetRelationRequest {

    /**
     * 关联人员主键列表。
     */
    @Schema(description = "关联人员主键列表")
    private List<Long> personIds;

    /**
     * 关联系统主键列表。
     */
    @Schema(description = "关联系统主键列表")
    private List<Long> informationSystemIds;

    /**
     * 关联项目主键列表。
     */
    @Schema(description = "关联项目主键列表")
    private List<Long> projectIds;

    /**
     * 关联服务商主键列表。
     */
    @Schema(description = "关联服务商主键列表")
    private List<Long> serviceProviderIds;
}
