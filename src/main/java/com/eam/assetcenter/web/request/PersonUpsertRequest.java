package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 人员新增或更新请求对象。
 */
@Data
@Schema(description = "人员新增或更新请求")
public class PersonUpsertRequest {

    /**
     * 姓名。
     */
    @Schema(description = "姓名")
    @NotBlank(message = "name is required")
    private String name;

    /**
     * 性别。
     */
    @Schema(description = "性别")
    private String gender;

    /**
     * 身份证号。
     */
    @Schema(description = "身份证号")
    private String idCardNo;

    /**
     * 手机号。
     */
    @Schema(description = "手机号")
    private String mobile;

    /**
     * 工号。
     */
    @Schema(description = "工号")
    private String employeeNo;

    /**
     * 照片地址。
     */
    @Schema(description = "照片地址")
    private String photoUrl;

    /**
     * 账号。
     */
    @Schema(description = "账号")
    private String account;

    /**
     * 所属部门主键。
     */
    @Schema(description = "所属部门主键")
    private Long departmentId;

    /**
     * 所属服务商主键。
     */
    @Schema(description = "所属服务商主键")
    private Long serviceProviderId;

    /**
     * 人员类型。
     */
    @Schema(description = "人员类型")
    private String personType;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    @NotBlank(message = "status is required")
    private String status;
}
