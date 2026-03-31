package com.eam.assetcenter.common.enums;

/**
 * 人员类型枚举。
 */
public enum PersonType {
    DEV("开发", "Development", "primary"),
    OPS("运维", "Operations", "success");

    private final String label;
    private final String labelEn;
    private final String tagType;

    PersonType(String label, String labelEn, String tagType) {
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
     * 判断人员类型值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (PersonType item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
