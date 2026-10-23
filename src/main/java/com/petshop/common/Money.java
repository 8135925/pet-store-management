package com.petshop.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * 金额工具：全库金额用「分」（int），本类负责 元/分 转换与折扣分摊。
 * 禁止 double/float/BigDecimal 入库，这里只在输入/输出边界使用 BigDecimal。
 */
public final class Money {

    private Money() {}

    /** 分 -> "128.00" */
    public static String yuan(int cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    /** "128.5" -> 12850；非法输入抛 BizException */
    public static int toCent(String yuan) {
        if (yuan == null || yuan.trim().isEmpty()) {
            return 0;
        }
        try {
            BigDecimal b = new BigDecimal(yuan.trim())
                    .setScale(2, RoundingMode.HALF_UP);
            return b.movePointRight(2).intValueExact();
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "金额格式不正确：" + yuan);
        }
    }

    /** 体重 kg 字符串 -> 克（整数） */
    public static Integer toGram(String kg) {
        if (kg == null || kg.trim().isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(kg.trim());
            if (v < 0) {
                throw new IllegalArgumentException();
            }
            return (int) Math.round(v * 1000);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "体重格式不正确：" + kg);
        }
    }

    /**
     * 按比例分摊，尾差归金额最大的明细。
     * baseAmounts 为「参与分摊」的明细金额数组（用次卡抵扣的明细已记 0，不参与）。
     */
    public static int[] allocate(int totalDiscount, int[] baseAmounts) {
        int n = baseAmounts.length;
        int[] result = new int[n];
        long base = 0;
        for (int a : baseAmounts) {
            base += a;
        }
        if (base <= 0) {
            return result;
        }

        int allocated = 0;
        for (int i = 0; i < n; i++) {
            result[i] = (int) Math.round((double) totalDiscount * baseAmounts[i] / base);
            allocated += result[i];
        }

        int diff = totalDiscount - allocated;
        if (diff != 0) {
            int maxIdx = 0;
            for (int i = 1; i < n; i++) {
                if (baseAmounts[i] > baseAmounts[maxIdx]) {
                    maxIdx = i;
                }
            }
            result[maxIdx] += diff;
        }
        return result;
    }

    /** 用于测试与调试 */
    public static int sum(int[] arr) {
        return Arrays.stream(arr).sum();
    }
}
