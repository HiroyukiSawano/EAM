package com.eam.assetcenter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 通用主键列表请求对象。
 */
@Data
@Schema(description = "通用主键列表请求")
public class IdListRequest {

    /**
     * 主键列表。
     */
    @Schema(description = "主键列表")
    @NotNull(message = "ids cannot be null")
    private List<Long> ids;
}
