package com.petshop.module.report;

import com.petshop.module.system.ConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final ConfigService configService;

    public ReportService(ReportMapper reportMapper, ConfigService configService) {
        this.reportMapper = reportMapper;
        this.configService = configService;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> m = reportMapper.dashboard(LocalDate.now());
        return m;
    }

    public Map<String, Object> dailySummary(LocalDate date) {
        Map<String, Object> m = reportMapper.dailySummary(date);
        return m;
    }

    public int expireWarnDays() {
        return configService.expireWarnDays();
    }
}
