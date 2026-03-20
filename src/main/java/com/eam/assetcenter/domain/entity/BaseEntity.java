package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 实体公共基类，封装审计与逻辑删除字段。
 */
@Data
@Schema(description = "实体公共字段")
public class BaseEntity {

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记。
     */
    @Schema(description = "逻辑删除标记")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
