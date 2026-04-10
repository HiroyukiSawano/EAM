package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 服务商新增或更新请求对象。
 */
@Data
@Schema(description = "服务商新增或更新请求")
public class ServiceProviderUpsertRequest {

    /**
     * 编码。
     */
    @Schema(description = "服务商编码")
    @NotBlank(message = "code is required")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "服务商名称")
    @NotBlank(message = "name is required")
    private String name;

    /**
     * 简称。
     */
    @Schema(description = "服务商简称")
    @NotBlank(message = "shortName is required")
    private String shortName;

    /**
     * Logo 地址。
     */
    @Schema(description = "Logo 地址")
    private String logoUrl;

    /**
     * 统一社会信用代码。
     */
    @Schema(description = "统一社会信用代码")
    @NotBlank(message = "unifiedSocialCreditCode is required")
    private String unifiedSocialCreditCode;

    /**
     * 旧版服务商类型，保留兼容。
     */
    @Schema(description = "旧版服务商类型")
    private String type;

    /**
     * 企业性质。
     */
    @Schema(description = "企业性质")
    @NotBlank(message = "enterpriseNature is required")
    private String enterpriseNature;

    /**
     * 合作范围代码列表。
     */
    @Schema(description = "合作范围代码列表")
    @NotEmpty(message = "cooperationScopes is required")
    private List<String> cooperationScopes;

    /**
     * 等级。
     */
    @Schema(description = "等级")
    @NotBlank(message = "vendorLevel is required")
    private String vendorLevel;

    /**
     * 评分。
     */
    @Schema(description = "评分")
    private Integer score;

    /**
     * 旧版评分等级，保留兼容。
     */
    @Schema(description = "旧版评分等级")
    private String ratingLevel;

    /**
     * 商务联系人。
     */
    @Schema(description = "商务联系人")
    @NotBlank(message = "businessContact is required")
    private String businessContact;

    /**
     * 商务联系电话。
     */
    @Schema(description = "商务联系电话")
    @NotBlank(message = "businessPhone is required")
    private String businessPhone;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    @NotBlank(message = "status is required")
    private String status;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 关联人员主键列表。
     */
    @Schema(description = "关联人员主键列表")
    private List<Long> personIds;

    /**
     * 关联信息系统主键列表。
     */
    @Schema(description = "关联信息系统主键列表")
    private List<Long> informationSystemIds;

    /**
     * 关联硬件资产主键列表。
     */
    @Schema(description = "关联硬件资产主键列表")
    private List<Long> hardwareAssetIds;
}
