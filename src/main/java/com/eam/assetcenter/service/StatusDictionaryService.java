package com.eam.assetcenter.service;

import com.eam.assetcenter.common.enums.CommonStatus;
import com.eam.assetcenter.common.enums.HardwareStatus;
import com.eam.assetcenter.common.enums.ProjectStatus;
import com.eam.assetcenter.common.enums.StatusDictionaryEnum;
import com.eam.assetcenter.common.model.StatusDictionaryItem;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 状态字典服务，统一输出各模块状态字典。
 */
@Service
public class StatusDictionaryService {

    /**
     * 查询全部状态字典分组。
     */
    public Map<String, List<StatusDictionaryItem>> listStatusDictionaries() {
        Map<String, List<StatusDictionaryItem>> result = new LinkedHashMap<String, List<StatusDictionaryItem>>();
        List<StatusDictionaryItem> commonStatuses = toDictionaryItems(CommonStatus.values());
        result.put("departmentStatus", commonStatuses);
        result.put("personStatus", commonStatuses);
        result.put("serviceProviderStatus", commonStatuses);
        result.put("informationSystemStatus", commonStatuses);
        result.put("projectStatus", toDictionaryItems(ProjectStatus.values()));
        result.put("hardwareStatus", toDictionaryItems(HardwareStatus.values()));
        return result;
    }

    private List<StatusDictionaryItem> toDictionaryItems(StatusDictionaryEnum[] items) {
        return Arrays.stream(items).map(StatusDictionaryEnum::toDictionaryItem).collect(Collectors.toList());
    }
}
