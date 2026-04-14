package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 信息系统新增或更新请求对象。
 */
@Data
@Schema(description = "信息系统新增或更新请求")
public class InformationSystemUpsertRequest {

    @Schema(description = "系统编码")
    @NotBlank(message = "code is required")
    private String code;

    @Schema(description = "系统名称")
    @NotBlank(message = "name is required")
    private String name;

    @Schema(description = "系统类型")
    @NotBlank(message = "systemType is required")
    private String systemType;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "部署架构")
    private String deploymentArchitecture;

    @Schema(description = "负责人主键")
    private Long ownerPersonId;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联服务商主键列表")
    private List<Long> serviceProviderIds;

    @Schema(description = "关联人员主键列表")
    private List<Long> personIds;

    @Schema(description = "关联硬件主键列表")
    private List<Long> hardwareAssetIds;
}
