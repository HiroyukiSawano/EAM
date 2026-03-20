package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 项目关联关系维护请求对象。
 */
@Data
@Schema(description = "项目关联关系请求")
public class ProjectRelationRequest {

    /**
     * 信息系统主键列表。
     */
    @Schema(description = "信息系统主键列表")
    private List<Long> informationSystemIds;

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
     * 硬件资产主键列表。
     */
    @Schema(description = "硬件资产主键列表")
    private List<Long> hardwareAssetIds;
}
