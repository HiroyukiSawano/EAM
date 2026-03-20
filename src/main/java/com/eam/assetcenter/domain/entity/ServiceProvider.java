package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务商实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_provider")
@Schema(description = "服务商")
public class ServiceProvider extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 编码。
     */
    @Schema(description = "服务商编码")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "服务商名称")
    private String name;

    /**
     * 统一社会信用代码。
     */
    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    /**
     * 类型。
     */
    @Schema(description = "服务商类型")
    private String type;

    /**
     * 评分等级。
     */
    @Schema(description = "评分等级")
    private String ratingLevel;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;
}
