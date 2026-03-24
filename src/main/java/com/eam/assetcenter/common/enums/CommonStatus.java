package com.eam.assetcenter.common.enums;

/**
 * 通用资源状态枚举。
 */
public enum CommonStatus implements StatusDictionaryEnum {
    ACTIVE("正常", "Active", "success"),
    INACTIVE("停用", "Inactive", "info");

    private final String label;
    private final String labelEn;
    private final String tagType;

    CommonStatus(String label, String labelEn, String tagType) {
        this.label = label;
        this.labelEn = labelEn;
        this.tagType = tagType;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getLabelEn() {
        return labelEn;
    }

    @Override
    public String getTagType() {
        return tagType;
    }

    /**
     * 判断状态值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (CommonStatus item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
