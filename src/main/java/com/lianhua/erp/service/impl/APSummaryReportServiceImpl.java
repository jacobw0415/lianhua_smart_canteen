package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.APSummaryReportDto;
import com.lianhua.erp.dto.report.ARSummaryReportDto;
import com.lianhua.erp.repository.APSummaryReportRepository;
import com.lianhua.erp.service.APSummaryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class APSummaryReportServiceImpl implements APSummaryReportService {

    private final APSummaryReportRepository repository;

    @Override
    public List<APSummaryReportDto> generateSummary(String period) {
        return repository.getSummaryList(period, null);
    }

    /**
     * 產生單一期間或截至特定日期的應付帳款報表
     */
    @Override
    public List<APSummaryReportDto> generateSummary(String period, String endDate) {
        // 直接呼叫 Repository 取得單一區間數據
        return repository.getSummaryList(period, endDate);
    }

    /**
     * 產生多個月份的趨勢比較報表，並自動計算合計列
     */
    @Override
    public List<APSummaryReportDto> generateSummary(List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 取得所有月份的數據
        // (因 Repository 已實作 getSummaryList(List<String>)，直接呼叫即可，
        //  若您的 Repository 沒有批次方法，亦可使用下方註解處的迴圈方式)
        List<APSummaryReportDto> list = new ArrayList<>(repository.getSummaryList(periods));

        /* * 若 Repository 未實作批次查詢，可使用此方式：
         * for (String p : periods) {
         * list.addAll(repository.getSummaryList(p, null));
         * }
         */

        if (list.isEmpty()) {
            return list;
        }

        // 2. 計算合計列 (Total Row)
        // 確保不重複添加合計 (以防萬一被呼叫多次)
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));

        if (!hasTotal) {
            APSummaryReportDto total = new APSummaryReportDto();
            total.setAccountingPeriod("合計");

            // 計算各欄位總和
            BigDecimal totalPayable = sum(list, APSummaryReportDto::getTotalPayable);
            BigDecimal totalPaid = sum(list, APSummaryReportDto::getTotalPaid);
            BigDecimal totalOutstanding = sum(list, APSummaryReportDto::getTotalOutstanding);

            total.setTotalPayable(totalPayable);
            total.setTotalPaid(totalPaid);
            total.setTotalOutstanding(totalOutstanding);

            // 將合計列加到列表最後
            list.add(total);
        }

        return list;
    }

    /**
     * 輔助方法：計算 BigDecimal 欄位總和，並處理 null 值
     */
    private BigDecimal sum(List<APSummaryReportDto> list, Function<APSummaryReportDto, BigDecimal> extractor) {
        return list.stream()
                .map(extractor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}