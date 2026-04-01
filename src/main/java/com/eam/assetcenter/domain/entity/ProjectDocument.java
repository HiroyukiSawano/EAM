package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目文档实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_document")
@Schema(description = "项目文档")
public class ProjectDocument extends BaseEntity {

    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    /**
     * 项目主键。
     */
    @Schema(description = "项目主键")
    private Long projectId;

    /**
     * 存储文件名。
     */
    @Schema(description = "存储文件名")
    private String fileName;

    /**
     * 原始文件名。
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 文件大小。
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    private String contentType;

    /**
     * 文件访问地址。
     */
    @Schema(description = "文件访问地址")
    private String fileUrl;

    /**
     * 上传时间。
     */
    @Schema(description = "上传时间")
    private LocalDateTime uploadedAt;
}
