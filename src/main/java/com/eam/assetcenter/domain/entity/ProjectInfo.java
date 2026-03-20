package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * 项目实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_info")
@Schema(description = "项目")
public class ProjectInfo extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 编码。
     */
    @Schema(description = "项目编码")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "项目名称")
    private String name;

    /**
     * 项目类型。
     */
    @Schema(description = "项目类型")
    private String projectType;

    /**
     * 项目状态。
     */
    @Schema(description = "项目状态")
    private String projectStatus;

    /**
     * 开始日期。
     */
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /**
     * 结束日期。
     */
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;
}
