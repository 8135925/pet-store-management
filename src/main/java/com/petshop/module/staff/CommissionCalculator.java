package com.petshop.module.staff;

import java.time.LocalDate;
import java.util.List;

/**
 * 提成计算与规则匹配。纯函数，便于单元测试。
 *
 * <p>红线：金额全部用「分」整数，比例用万分比整数，全流程无浮点（BR-20）。</p>
 */
public final class CommissionCalculator {

    private CommissionCalculator() {
    }

    /**
     * 提成金额 = round(基数 × 万分比 / 10000) + 固定额。
     * 基数为 0 或负数（卡抵扣行）时不提成。
     */
    public static int calc(int baseCent, int ratioBp, int fixedCent) {
        if (baseCent <= 0) {
            return 0;
        }
        if (ratioBp < 0 || fixedCent < 0) {
            return 0;
        }
        long raw = (long) baseCent * ratioBp;
        long rounded = (raw + 5000) / 10000;   // 四舍五入到分
        long total = rounded + fixedCent;
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    /**
     * 规则匹配优先级：指定项目 > 同类型（不限项目）> ALL；同优先级取生效日更晚的一条。
     * 无匹配返回 null（不生成记录，不报错）。
     */
    public static CommissionRule pick(List<CommissionRule> rules, String scope, Long refId,
                                      String staffRole, LocalDate date) {
        if (rules == null || rules.isEmpty() || date == null) {
            return null;
        }
        CommissionRule best = null;
        int bestScore = -1;
        for (CommissionRule r : rules) {
            if (r == null || !r.getIsActive()) {
                continue;
            }
            if (r.getEffectiveFrom() != null && r.getEffectiveFrom().isAfter(date)) {
                continue;
            }
            if (r.getEffectiveTo() != null && r.getEffectiveTo().isBefore(date)) {
                continue;
            }
            if (r.getStaffRole() != null && !r.getStaffRole().trim().isEmpty()
                    && !r.getStaffRole().equals(staffRole)) {
                continue;
            }
            boolean scopeMatch = scope != null && scope.equals(r.getScope());
            boolean all = "ALL".equals(r.getScope());
            if (!scopeMatch && !all) {
                continue;
            }
            if (scopeMatch && r.getRefId() != null && !r.getRefId().equals(refId)) {
                continue;   // 指定了别的项目，不适用
            }
            int score;
            if (scopeMatch && r.getRefId() != null && r.getRefId().equals(refId)) {
                score = 100;
            } else if (scopeMatch && r.getRefId() == null) {
                score = 50;
            } else {
                score = 10;
            }
            if (score > bestScore || (score == bestScore && later(r, best))) {
                best = r;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean later(CommissionRule a, CommissionRule b) {
        if (b == null) {
            return true;
        }
        if (a.getEffectiveFrom() == null) {
            return false;
        }
        if (b.getEffectiveFrom() == null) {
            return true;
        }
        return a.getEffectiveFrom().isAfter(b.getEffectiveFrom());
    }
}
