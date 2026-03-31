package com.eam.assetcenter.common.enums;

/**
 * 服务商企业性质枚举。
 */
public enum EnterpriseNature {
    CENTRAL_STATE_OWNED("央企", "Central State-Owned", "danger"),
    STATE_OWNED("国企", "State-Owned", "warning"),
    PRIVATE("民企", "Private Enterprise", "success"),
    PUBLIC_INSTITUTION("事业单位", "Public Institution", "info");

    private final String label;
    private final String labelEn;
    private final String tagType;

    EnterpriseNature(String label, String labelEn, String tagType) {
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
     * 判断企业性质值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (EnterpriseNature item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
