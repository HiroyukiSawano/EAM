package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("person")
@Schema(description = "人员")
public class Person extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 名称。
     */
    @Schema(description = "姓名")
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
     * 是否开通运营账号。
     */
    @Schema(description = "是否开通运营账号")
    private Boolean hasOpsAccount;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;
}
