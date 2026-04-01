package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目新增或更新请求对象。
 */
@Data
@Schema(description = "项目新增或更新请求")
public class ProjectUpsertRequest {

    @Schema(description = "项目编码")
    @NotBlank(message = "code is required")
    private String code;

    @Schema(description = "项目名称")
    @NotBlank(message = "name is required")
    private String name;

    @Schema(description = "项目类型")
    @NotBlank(message = "projectType is required")
    private String projectType;

    @Schema(description = "项目状态")
    @NotBlank(message = "projectStatus is required")
    private String projectStatus;

    @Schema(description = "立项批号")
    private String approvalBatchNo;

    @Schema(description = "项目预算（万元）")
    private BigDecimal projectBudget;

    @Schema(description = "项目合同金额（万元）")
    private BigDecimal contractAmount;

    @Schema(description = "项目负责人姓名")
    private String ownerName;

    @Schema(description = "项目负责人联系电话")
    private String ownerPhone;

    @Schema(description = "立项时间")
    private LocalDate approvalDate;

    @Schema(description = "开工时间")
    private LocalDate startDate;

    @Schema(description = "初验时间")
    private LocalDate initialDeliveryDate;

    @Schema(description = "终验时间")
    private LocalDate endDate;

    @Schema(description = "质保截止时间")
    private LocalDate warrantyEndDate;

    @Schema(description = "当前阶段")
    private String stage;

    @Schema(description = "周期名称")
    private String paymentCycleName;

    @Schema(description = "付款比例")
    private BigDecimal paymentRatio;

    @Schema(description = "付款金额")
    private BigDecimal paymentAmount;

    @Schema(description = "计划付款时间")
    private LocalDate plannedPaymentDate;

    @Schema(description = "实际付款时间")
    private LocalDate actualPaymentDate;

    @Schema(description = "付款状态")
    private String paymentStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "项目文档列表")
    private List<ProjectDocumentRequest> documents;

    @Schema(description = "关联人员主键列表")
    private List<Long> personIds;

    @Schema(description = "关联信息系统主键列表")
    private List<Long> informationSystemIds;

    @Schema(description = "关联硬件资产主键列表")
    private List<Long> hardwareAssetIds;
}
