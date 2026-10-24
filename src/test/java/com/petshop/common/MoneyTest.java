package com.petshop.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 折扣分摊：尾差归金额最大明细（设计 §8.4 / §15.1） */
public class MoneyTest {

    @Test
    public void allocateSumExactAndBalanced() {
        // 1000 分摊到 [3333,3333,3334]：合计恰为 1000，各项互差 ≤ 1
        int[] r = Money.allocate(1000, new int[]{3333, 3333, 3334});
        assertEquals(1000, r[0] + r[1] + r[2]);
        int max = Math.max(r[0], Math.max(r[1], r[2]));
        int min = Math.min(r[0], Math.min(r[1], r[2]));
        assertEquals(1, max - min);
    }

    @Test
    public void sumExactOnOddSplit() {
        int[] r = Money.allocate(10, new int[]{3333, 3333, 3334});
        assertEquals(10, r[0] + r[1] + r[2]);
        int[] r2 = Money.allocate(100, new int[]{10000, 10000, 10001});
        assertEquals(100, r2[0] + r2[1] + r2[2]);
    }

    @Test
    public void zeroBaseReturnsZeros() {
        assertArrayEquals(new int[]{0, 0}, Money.allocate(100, new int[]{0, 0}));
    }

    @Test
    public void yuanFormat() {
        assertEquals("128.00", Money.yuan(12800));
        assertEquals("0.01", Money.yuan(1));
        assertEquals(12850, Money.toCent("128.5"));
        assertEquals(0, Money.toCent(""));
    }
}
