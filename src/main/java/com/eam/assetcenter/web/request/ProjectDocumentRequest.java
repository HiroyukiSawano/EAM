package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目文档请求对象。
 */
@Data
@Schema(description = "项目文档请求")
public class ProjectDocumentRequest {

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
}
