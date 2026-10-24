package com.petshop.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 营业日切点：03:59 → 前一天；04:00 → 当天（需求 §3.4） */
public class BusinessDateTest {

    @Test
    public void beforeCutoffBelongsToYesterday() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 24, 3, 59);
        assertEquals(LocalDate.of(2026, 10, 23), BusinessDate.of(t));
    }

    @Test
    public void atCutoffBelongsToToday() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 24, 4, 0);
        assertEquals(LocalDate.of(2026, 10, 24), BusinessDate.of(t));
    }

    @Test
    public void midnightBelongsToYesterday() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 24, 0, 0);
        assertEquals(LocalDate.of(2026, 10, 23), BusinessDate.of(t));
    }

    @Test
    public void normalDaytimeBelongsToToday() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 24, 21, 30);
        assertEquals(LocalDate.of(2026, 10, 24), BusinessDate.of(t));
    }
}
