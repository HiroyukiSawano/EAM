package com.eam.assetcenter.common.enums;

/**
 * 服务商合作范围枚举。
 */
public enum CooperationScope {
    HARDWARE_PROCUREMENT("硬件采购", "Hardware Procurement", "warning"),
    SOFTWARE_DEVELOPMENT("软件开发", "Software Development", "primary"),
    OPERATIONS_SERVICE("运维服务", "Operations Service", "success"),
    INTEGRATION("集成服务", "Integration Service", "info");

    private final String label;
    private final String labelEn;
    private final String tagType;

    CooperationScope(String label, String labelEn, String tagType) {
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
     * 判断合作范围值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (CooperationScope item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
