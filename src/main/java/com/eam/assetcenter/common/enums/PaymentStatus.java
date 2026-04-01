package com.eam.assetcenter.common.enums;

/**
 * 项目付款状态枚举。
 */
public enum PaymentStatus implements StatusDictionaryEnum {
    PENDING("待支付", "Pending", "info"),
    PARTIAL("部分支付", "Partial", "warning"),
    PAID("已付款", "Paid", "success");

    private final String label;
    private final String labelEn;
    private final String tagType;

    PaymentStatus(String label, String labelEn, String tagType) {
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
     * 判断付款状态是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (PaymentStatus item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
