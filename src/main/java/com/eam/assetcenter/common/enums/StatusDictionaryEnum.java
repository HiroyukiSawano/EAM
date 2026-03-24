package com.eam.assetcenter.common.enums;

import com.eam.assetcenter.common.model.StatusDictionaryItem;

/**
 * 状态字典枚举统一约定。
 */
public interface StatusDictionaryEnum {

    /**
     * 获取状态代码值。
     */
    String getValue();

    /**
     * 获取中文标签。
     */
    String getLabel();

    /**
     * 获取英文标签。
     */
    String getLabelEn();

    /**
     * 获取标签样式类型。
     */
    String getTagType();

    /**
     * 转换为字典项对象。
     */
    default StatusDictionaryItem toDictionaryItem() {
        return new StatusDictionaryItem(getValue(), getLabel(), getLabelEn(), getTagType());
    }
}
