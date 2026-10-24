package com.petshop.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** 营业日计算：凌晨 04:00 切点，00:00–03:59 归属前一天。 */
public final class BusinessDate {

    /** 营业日切点小时，默认 04:00；由 app.business-day-cutoff-hour 注入 */
    private static volatile int cutoffHour = 4;
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BusinessDate() {}

    public static void setCutoffHour(int hour) {
        if (hour >= 0 && hour <= 23) {
            cutoffHour = hour;
        }
    }

    public static LocalDate of(LocalDateTime now) {
        return now.getHour() < cutoffHour
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
