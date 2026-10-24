package com.petshop.module.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FEFO（先到期先出）分配算法。
 *
 * <p>纯函数，不碰数据库：输入的批次顺序由 SQL 按「有效期升序、入库时间升序」排好，
 * 这里只负责把需求量切成若干段。这样出库规则可以被单元测试完全覆盖。</p>
 */
public final class BatchAllocator {

    private BatchAllocator() {
    }

    /** 一个可用批次（只取分配需要的字段） */
    public static final class BatchSlice {
        private final Long batchId;
        private final int qtyLeft;
        private final int unitCostCent;

        public BatchSlice(Long batchId, int qtyLeft, int unitCostCent) {
            this.batchId = batchId;
            this.qtyLeft = qtyLeft;
            this.unitCostCent = unitCostCent;
        }

        public Long getBatchId() { return batchId; }
        public int getQtyLeft() { return qtyLeft; }
        public int getUnitCostCent() { return unitCostCent; }
    }

    /** 一个分配结果 */
    public static final class Allocation {
        private final Long batchId;
        private final int qty;
        private final int unitCostCent;

        public Allocation(Long batchId, int qty, int unitCostCent) {
            this.batchId = batchId;
            this.qty = qty;
            this.unitCostCent = unitCostCent;
        }

        public Long getBatchId() { return batchId; }
        public int getQty() { return qty; }
        public int getUnitCostCent() { return unitCostCent; }
    }

    /** 分配结果：shortage > 0 表示批次总量不足 */
    public static final class Result {
        private final List<Allocation> allocations;
        private final int shortage;

        Result(List<Allocation> allocations, int shortage) {
            this.allocations = allocations;
            this.shortage = shortage;
        }

        public List<Allocation> getAllocations() { return allocations; }
        public int getShortage() { return shortage; }
        public boolean isComplete() { return shortage == 0; }
    }

    /**
     * 按给定顺序分配。
     *
     * @param batches 已按 FEFO 排好序的可用批次
     * @param qty     需求数量，必须 > 0
     */
    public static Result allocate(List<BatchSlice> batches, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("分配数量必须大于 0");
        }
        if (batches == null || batches.isEmpty()) {
            return new Result(Collections.<Allocation>emptyList(), qty);
        }
        List<Allocation> out = new ArrayList<Allocation>();
        int remaining = qty;
        for (BatchSlice b : batches) {
            if (remaining == 0) {
                break;
            }
            if (b == null || b.getQtyLeft() <= 0) {
                continue;
            }
            int take = Math.min(remaining, b.getQtyLeft());
            out.add(new Allocation(b.getBatchId(), take, b.getUnitCostCent()));
            remaining -= take;
        }
        return new Result(out, remaining);
    }
}
