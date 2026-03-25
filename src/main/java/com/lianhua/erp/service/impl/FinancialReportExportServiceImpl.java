package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.report.*;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialReportExportServiceImpl implements FinancialReportExportService {

    private static final int MAX_PERIODS = 12;
    private static final int EXPORT_PAGE_MAX_SIZE = 5000;

    private record ColDef(String key, String headerZh) {}

    private static final List<ColDef> COLS_BALANCE_SHEET = List.of(
            new ColDef("accountingPeriod", "會計期間"),
            new ColDef("accountsReceivable", "應收帳款"),
            new ColDef("cash", "現金"),
            new ColDef("accountsPayable", "應付帳款"),
            new ColDef("totalAssets", "總資產"),
            new ColDef("totalLiabilities", "總負債"),
            new ColDef("equity", "業主權益")
    );

    private static final List<ColDef> COLS_COMPREHENSIVE = List.of(
            new ColDef("accountingPeriod", "會計期間"),
            new ColDef("retailSales", "零售銷售收入"),
            new ColDef("orderSales", "訂單銷售收入"),
            new ColDef("totalRevenue", "營業收入合計"),
            new ColDef("costOfGoodsSold", "營業成本"),
            new ColDef("grossProfit", "毛利益"),
            new ColDef("totalOperatingExpenses", "營業費用合計"),
            new ColDef("operatingProfit", "營業利益"),
            new ColDef("otherIncome", "其他收入"),
            new ColDef("otherExpenses", "其他支出"),
            new ColDef("netProfit", "本期淨利"),
            new ColDef("otherComprehensiveIncome", "其他綜合損益"),
            new ColDef("comprehensiveIncome", "綜合損益總額")
    );

    private static final List<ColDef> COLS_CASH_FLOW = List.of(
            new ColDef("accountingPeriod", "會計期間"),
            new ColDef("totalSales", "零售現金收入"),
            new ColDef("totalReceipts", "訂單收款收入"),
            new ColDef("totalPayments", "採購付款支出"),
            new ColDef("totalExpenses", "營運費用支出"),
            new ColDef("totalInflow", "總流入"),
            new ColDef("totalOutflow", "總流出"),
            new ColDef("netCashFlow", "淨現金流")
    );

    private static final List<ColDef> COLS_AR = List.of(
            new ColDef("accountingPeriod", "會計期間"),
            new ColDef("totalReceivable", "應收總額"),
            new ColDef("totalReceived", "已收金額"),
            new ColDef("totalOutstanding", "未收餘額")
    );

    private static final List<ColDef> COLS_AP = List.of(
            new ColDef("accountingPeriod", "會計期間"),
            new ColDef("totalPayable", "應付總額"),
            new ColDef("totalPaid", "已付金額"),
            new ColDef("totalOutstanding", "未付餘額")
    );

    private final BalanceSheetReportService balanceSheetReportService;
    private final ComprehensiveIncomeStatementService comprehensiveIncomeStatementService;
    private final CashFlowReportService cashFlowReportService;
    private final ARSummaryReportService arSummaryReportService;
    private final APSummaryReportService apSummaryReportService;

    @Override
    @Transactional(readOnly = true)
    public ExportPayload export(
            FinancialReportKey reportKey,
            ReportExportQueryDto query,
            String periodsCommaSeparated,
            ExportFormat format,
            ExportScope scope,
            Pageable pageable,
            String columnsCsv
    ) {
        ReportExportQueryDto q = query == null ? new ReportExportQueryDto() : query;
        mergeCommaPeriods(q, periodsCommaSeparated);

        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;
        Pageable p = pageable == null ? Pageable.unpaged() : pageable;

        List<ColDef> columns = resolveColumns(reportKey, columnsCsv);
        String[] headers = columns.stream().map(ColDef::headerZh).toArray(String[]::new);

        List<String[]> rows = switch (reportKey) {
            case BALANCE_SHEET -> exportBalanceSheet(q, columns, safeScope, p);
            case COMPREHENSIVE_INCOME_STATEMENT -> exportComprehensive(q, columns, safeScope, p);
            case CASH_FLOW_REPORTS -> exportCashFlow(q, columns, safeScope, p);
            case AR_SUMMARY -> exportArSummary(q, columns, safeScope, p);
            case AP_SUMMARY -> exportApSummary(q, columns, safeScope, p);
        };

        byte[] data = safeFormat == ExportFormat.CSV
                ? TabularExporter.toCsvUtf8Bom(headers, rows)
                : TabularExporter.toXlsx(sheetName(reportKey), headers, rows);

        String filename = ExportFilenameUtils.build(reportKey.pathSegment(), safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private static String sheetName(FinancialReportKey key) {
        String n = switch (key) {
            case BALANCE_SHEET -> "資產負債表";
            case COMPREHENSIVE_INCOME_STATEMENT -> "綜合損益表";
            case CASH_FLOW_REPORTS -> "現金流量表";
            case AR_SUMMARY -> "應收帳款總表";
            case AP_SUMMARY -> "應付帳款總表";
        };
        return n.length() <= 31 ? n : n.substring(0, 31);
    }

    private List<String[]> exportBalanceSheet(
            ReportExportQueryDto q,
            List<ColDef> columns,
            ExportScope scope,
            Pageable pageable
    ) {
        List<String> pl = q.resolvedPeriodsList();
        validatePeriodsCount(pl);

        List<BalanceSheetReportDto> data;
        if (pl.size() > 1) {
            data = balanceSheetReportService.generateBalanceSheet(pl);
        } else if (pl.size() == 1) {
            data = balanceSheetReportService.generateBalanceSheet(pl.get(0), q.getEndDate());
        } else {
            data = balanceSheetReportService.generateBalanceSheet(q.getPeriod(), q.getEndDate());
        }

        data = applyAccountingPeriodSort(data, pageable.getSort(), BalanceSheetReportDto::getAccountingPeriod);
        data = applyPageSlice(data, scope, pageable);
        return data.stream().map(d -> rowBalanceSheet(d, columns)).toList();
    }

    private List<String[]> exportComprehensive(
            ReportExportQueryDto q,
            List<ColDef> columns,
            ExportScope scope,
            Pageable pageable
    ) {
        List<String> pl = q.resolvedPeriodsList();
        validatePeriodsCount(pl);

        List<ComprehensiveIncomeStatementDto> data;
        if (pl.size() > 1) {
            data = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(pl);
        } else if (pl.size() == 1) {
            data = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(
                    pl.get(0), q.getStartDate(), q.getEndDate());
        } else {
            data = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(
                    q.getPeriod(), q.getStartDate(), q.getEndDate());
        }

        data = applyAccountingPeriodSort(data, pageable.getSort(), ComprehensiveIncomeStatementDto::getAccountingPeriod);
        data = applyPageSlice(data, scope, pageable);
        return data.stream().map(d -> rowComprehensive(d, columns)).toList();
    }

    private List<String[]> exportCashFlow(
            ReportExportQueryDto q,
            List<ColDef> columns,
            ExportScope scope,
            Pageable pageable
    ) {
        List<CashFlowReportDto> data = cashFlowReportService.generateCashFlow(
                q.getPeriod(), q.getStartDate(), q.getEndDate());
        data = applyAccountingPeriodSort(data, pageable.getSort(), CashFlowReportDto::getAccountingPeriod);
        data = applyPageSlice(data, scope, pageable);
        return data.stream().map(d -> rowCashFlow(d, columns)).toList();
    }

    private List<String[]> exportArSummary(
            ReportExportQueryDto q,
            List<ColDef> columns,
            ExportScope scope,
            Pageable pageable
    ) {
        List<String> pl = q.resolvedPeriodsList();
        validatePeriodsCount(pl);

        List<ARSummaryReportDto> data;
        if (pl.size() > 1) {
            data = arSummaryReportService.generateSummary(pl);
        } else if (pl.size() == 1) {
            data = arSummaryReportService.generateSummary(pl.get(0), q.getEndDate());
        } else {
            data = arSummaryReportService.generateSummary(q.getPeriod(), q.getEndDate());
        }

        data = applyAccountingPeriodSort(data, pageable.getSort(), ARSummaryReportDto::getAccountingPeriod);
        data = applyPageSlice(data, scope, pageable);
        return data.stream().map(d -> rowAr(d, columns)).toList();
    }

    private List<String[]> exportApSummary(
            ReportExportQueryDto q,
            List<ColDef> columns,
            ExportScope scope,
            Pageable pageable
    ) {
        List<String> pl = q.resolvedPeriodsList();
        validatePeriodsCount(pl);

        List<APSummaryReportDto> data;
        if (pl.size() > 1) {
            data = apSummaryReportService.generateSummary(pl);
        } else if (pl.size() == 1) {
            data = apSummaryReportService.generateSummary(pl.get(0), q.getEndDate());
        } else {
            data = apSummaryReportService.generateSummary(q.getPeriod(), q.getEndDate());
        }

        data = applyAccountingPeriodSort(data, pageable.getSort(), APSummaryReportDto::getAccountingPeriod);
        data = applyPageSlice(data, scope, pageable);
        return data.stream().map(d -> rowAp(d, columns)).toList();
    }

    private static void mergeCommaPeriods(ReportExportQueryDto q, String periodsCommaSeparated) {
        if (periodsCommaSeparated == null || periodsCommaSeparated.isBlank()) {
            return;
        }
        if (q.getPeriods() != null && !q.getPeriods().isEmpty()) {
            return;
        }
        List<String> parsed = Arrays.stream(periodsCommaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        q.setPeriods(parsed);
    }

    private static void validatePeriodsCount(List<String> periods) {
        if (periods != null && periods.size() > MAX_PERIODS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "periods 最多 " + MAX_PERIODS + " 個");
        }
    }

    private static List<ColDef> allColumns(FinancialReportKey reportKey) {
        return switch (reportKey) {
            case BALANCE_SHEET -> COLS_BALANCE_SHEET;
            case COMPREHENSIVE_INCOME_STATEMENT -> COLS_COMPREHENSIVE;
            case CASH_FLOW_REPORTS -> COLS_CASH_FLOW;
            case AR_SUMMARY -> COLS_AR;
            case AP_SUMMARY -> COLS_AP;
        };
    }

    private static List<ColDef> resolveColumns(FinancialReportKey reportKey, String columnsCsv) {
        List<ColDef> all = allColumns(reportKey);
        Map<String, ColDef> byKey = all.stream().collect(Collectors.toMap(ColDef::key, c -> c, (a, b) -> a, LinkedHashMap::new));
        if (columnsCsv == null || columnsCsv.isBlank()) {
            return all;
        }
        List<String> requested = Arrays.stream(columnsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (requested.isEmpty()) {
            return all;
        }
        List<ColDef> out = new ArrayList<>();
        for (String k : requested) {
            ColDef c = byKey.get(k);
            if (c == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "不支援的欄位鍵: " + k + "（reportKey=" + reportKey.pathSegment() + "）");
            }
            out.add(c);
        }
        return out;
    }

    private static <T> List<T> applyAccountingPeriodSort(
            List<T> data,
            Sort sort,
            Function<T, String> accountingPeriod
    ) {
        if (sort == null || sort.isUnsorted()) {
            return new ArrayList<>(data);
        }
        Sort.Order o = sort.iterator().next();
        if (!"accountingPeriod".equals(o.getProperty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort 僅支援 accountingPeriod");
        }
        List<T> copy = new ArrayList<>(data);
        copy.sort(Comparator.comparing(accountingPeriod, Comparator.nullsLast(String::compareTo)));
        if (o.isDescending()) {
            Collections.reverse(copy);
        }
        return copy;
    }

    private static <T> List<T> applyPageSlice(List<T> sorted, ExportScope scope, Pageable pageable) {
        if (scope != ExportScope.PAGE) {
            return sorted;
        }
        Pageable p = pageable == null ? PageRequest.of(0, 20) : pageable;
        int page = Math.max(p.getPageNumber(), 0);
        int sliceSize;
        if (p.isUnpaged() || p.getPageSize() <= 0) {
            sliceSize = 20;
        } else {
            sliceSize = Math.min(p.getPageSize(), EXPORT_PAGE_MAX_SIZE);
        }
        int from = page * sliceSize;
        if (from >= sorted.size()) {
            return List.of();
        }
        int endIndex = Math.min(from + sliceSize, sorted.size());
        return new ArrayList<>(sorted.subList(from, endIndex));
    }

    private static String cell(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static String txt(String v) {
        return v == null ? "" : v;
    }

    private static String[] rowBalanceSheet(BalanceSheetReportDto d, List<ColDef> cols) {
        String[] r = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            r[i] = switch (cols.get(i).key()) {
                case "accountingPeriod" -> txt(d.getAccountingPeriod());
                case "accountsReceivable" -> cell(d.getAccountsReceivable());
                case "cash" -> cell(d.getCash());
                case "accountsPayable" -> cell(d.getAccountsPayable());
                case "totalAssets" -> cell(d.getTotalAssets());
                case "totalLiabilities" -> cell(d.getTotalLiabilities());
                case "equity" -> cell(d.getEquity());
                default -> "";
            };
        }
        return r;
    }

    private static String[] rowComprehensive(ComprehensiveIncomeStatementDto d, List<ColDef> cols) {
        String[] r = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            r[i] = switch (cols.get(i).key()) {
                case "accountingPeriod" -> txt(d.getAccountingPeriod());
                case "retailSales" -> cell(d.getRetailSales());
                case "orderSales" -> cell(d.getOrderSales());
                case "totalRevenue" -> cell(d.getTotalRevenue());
                case "costOfGoodsSold" -> cell(d.getCostOfGoodsSold());
                case "grossProfit" -> cell(d.getGrossProfit());
                case "totalOperatingExpenses" -> cell(d.getTotalOperatingExpenses());
                case "operatingProfit" -> cell(d.getOperatingProfit());
                case "otherIncome" -> cell(d.getOtherIncome());
                case "otherExpenses" -> cell(d.getOtherExpenses());
                case "netProfit" -> cell(d.getNetProfit());
                case "otherComprehensiveIncome" -> cell(d.getOtherComprehensiveIncome());
                case "comprehensiveIncome" -> cell(d.getComprehensiveIncome());
                default -> "";
            };
        }
        return r;
    }

    private static String[] rowCashFlow(CashFlowReportDto d, List<ColDef> cols) {
        String[] r = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            r[i] = switch (cols.get(i).key()) {
                case "accountingPeriod" -> txt(d.getAccountingPeriod());
                case "totalSales" -> cell(d.getTotalSales());
                case "totalReceipts" -> cell(d.getTotalReceipts());
                case "totalPayments" -> cell(d.getTotalPayments());
                case "totalExpenses" -> cell(d.getTotalExpenses());
                case "totalInflow" -> cell(d.getTotalInflow());
                case "totalOutflow" -> cell(d.getTotalOutflow());
                case "netCashFlow" -> cell(d.getNetCashFlow());
                default -> "";
            };
        }
        return r;
    }

    private static String[] rowAr(ARSummaryReportDto d, List<ColDef> cols) {
        String[] r = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            r[i] = switch (cols.get(i).key()) {
                case "accountingPeriod" -> txt(d.getAccountingPeriod());
                case "totalReceivable" -> cell(d.getTotalReceivable());
                case "totalReceived" -> cell(d.getTotalReceived());
                case "totalOutstanding" -> cell(d.getTotalOutstanding());
                default -> "";
            };
        }
        return r;
    }

    private static String[] rowAp(APSummaryReportDto d, List<ColDef> cols) {
        String[] r = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            r[i] = switch (cols.get(i).key()) {
                case "accountingPeriod" -> txt(d.getAccountingPeriod());
                case "totalPayable" -> cell(d.getTotalPayable());
                case "totalPaid" -> cell(d.getTotalPaid());
                case "totalOutstanding" -> cell(d.getTotalOutstanding());
                default -> "";
            };
        }
        return r;
    }
}
