package com.petshop.module.crm;

import java.util.LinkedHashMap;
import java.util.Map;

/** 标签字典：固定编码 + 中文显示，避免门店自由发挥导致筛选失效。 */
public final class TagLabels {

    public static final Map<String, String> PET_TAGS = new LinkedHashMap<String, String>();
    public static final Map<String, String> CUSTOMER_TAGS = new LinkedHashMap<String, String>();

    static {
        PET_TAGS.put("AGGRESSIVE", "凶猛");
        PET_TAGS.put("FEAR_DRYER", "怕吹风");
        PET_TAGS.put("SENIOR", "老年");
        PET_TAGS.put("SKIN_SENSITIVE", "皮肤敏感");
        PET_TAGS.put("WASH_ONLY", "只洗不剪");
        PET_TAGS.put("PREGNANT", "孕期");
        PET_TAGS.put("DISABLED", "行动不便");
        PET_TAGS.put("OTHER", "其他");

        CUSTOMER_TAGS.put("PRICE_SENSITIVE", "价格敏感");
        CUSTOMER_TAGS.put("HIGH_VALUE", "高价值");
        CUSTOMER_TAGS.put("COMPLAINED", "投诉过");
        CUSTOMER_TAGS.put("REFERRAL", "转介绍");
    }

    private TagLabels() {
    }

    public static String petLabel(String code) {
        String s = PET_TAGS.get(code);
        return s == null ? code : s;
    }

    public static String customerLabel(String code) {
        String s = CUSTOMER_TAGS.get(code);
        return s == null ? code : s;
    }

    public static boolean isValidPet(String code) {
        return PET_TAGS.containsKey(code);
    }

    public static boolean isValidCustomer(String code) {
        return CUSTOMER_TAGS.containsKey(code);
    }
}
