package com.eam.assetcenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产位置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_location")
@Schema(description = "资产位置")
public class AssetLocation extends BaseEntity {

    /**
     * 主键。
     */
    @Schema(description = "主键")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 编码。
     */
    @Schema(description = "位置编码")
    private String code;

    /**
     * 名称。
     */
    @Schema(description = "位置名称")
    private String name;

    /**
     * 场所。
     */
    @Schema(description = "场所")
    private String site;

    /**
     * 楼栋。
     */
    @Schema(description = "楼栋")
    private String building;

    /**
     * 楼层。
     */
    @Schema(description = "楼层")
    private String floor;

    /**
     * 区域。
     */
    @Schema(description = "区域")
    private String area;

    /**
     * 详细地址。
     */
    @Schema(description = "详细地址")
    private String addressDetail;
}
