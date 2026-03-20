package com.eam.assetcenter.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private long pageNo;
    private long pageSize;
    private List<T> records;

    public static <T> PageResponse<T> from(IPage<T> page) {
        return new PageResponse<T>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}


