package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 人员关联关系维护请求对象。
 */
@Data
@Schema(description = "人员关联关系请求")
public class PersonRelationRequest {

    /**
     * 硬件资产主键列表。
     */
    @Schema(description = "硬件资产主键列表")
    private List<Long> hardwareAssetIds;

    /**
     * 信息系统主键列表。
     */
    @Schema(description = "信息系统主键列表")
    private List<Long> informationSystemIds;

    /**
     * 项目主键列表。
     */
    @Schema(description = "项目主键列表")
    private List<Long> projectIds;

    /**
     * 兼容保留字段，人员单运营商规则下禁止写入。
     */
    @Schema(description = "关联服务商主键列表（兼容保留，当前禁止写入）")
    private List<Long> relatedServiceProviderIds;
}
