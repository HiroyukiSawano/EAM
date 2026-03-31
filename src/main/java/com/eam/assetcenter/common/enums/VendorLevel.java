package com.eam.assetcenter.common.enums;

/**
 * 服务商等级枚举。
 */
public enum VendorLevel {
    STRATEGIC_PARTNER("战略伙伴", "Strategic Partner", "danger"),
    CORE_SUPPLIER("核心供应商", "Core Supplier", "warning"),
    GENERAL_SUPPLIER("普通供应商", "General Supplier", "info");

    private final String label;
    private final String labelEn;
    private final String tagType;

    VendorLevel(String label, String labelEn, String tagType) {
        this.label = label;
        this.labelEn = labelEn;
        this.tagType = tagType;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getTagType() {
        return tagType;
    }

    /**
     * 判断服务商等级值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (VendorLevel item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
