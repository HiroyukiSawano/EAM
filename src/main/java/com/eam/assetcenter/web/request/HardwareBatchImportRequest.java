package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 硬件资产批量导入请求对象。
 */
@Data
@Schema(description = "硬件资产批量导入请求")
public class HardwareBatchImportRequest {

    /**
     * 导入项列表。
     */
    @Schema(description = "导入项列表")
    @Valid
    @NotEmpty(message = "items cannot be empty")
    private List<HardwareAssetUpsertRequest> items;
}
