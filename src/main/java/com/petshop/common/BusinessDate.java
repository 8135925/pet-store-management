package com.petshop.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** 营业日计算：凌晨 04:00 切点，00:00–03:59 归属前一天。 */
public final class BusinessDate {

    private static final LocalTime CUTOFF = LocalTime.of(4, 0);
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BusinessDate() {}

    public static LocalDate of(LocalDateTime now) {
        return now.toLocalTime().isBefore(CUTOFF)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();
    }

    public static LocalDate today() {
        return of(LocalDateTime.now());
    }

    public static String fmt(LocalDate d) {
        return d == null ? null : d.format(DATE);
    }

    public static String fmt(LocalDateTime t) {
        return t == null ? null : t.format(TIME);
    }
}
