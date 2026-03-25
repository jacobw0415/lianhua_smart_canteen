package com.lianhua.erp.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * 將表格式資料（表頭 + 每列字串陣列）寫成 xlsx 或 CSV。
 */
public final class TabularExporter {

    private static final short LIANHUA_GREEN = IndexedColors.GREEN.getIndex();
    private static final int AUTO_SIZE_ROW_THRESHOLD = 2000;
    /**
     * 大量列時取樣筆數，用於估算欄寬（避免全表掃描）。
     */
    private static final int WIDTH_SAMPLE_ROW_CAP = 800;
    /** Excel 欄寬上限（POI：字元寬度 × 256） */
    private static final int MAX_COL_WIDTH_UNITS = 255 * 256;
    /** 中日文等欄位最小「顯示單位」對應欄寬（字元）之下限，避免表頭被截成「…」 */
    private static final int MIN_WIDTH_CHARS_FLOOR = 16;
    /** 在內容估算寬度上再預留的字元數 */
    private static final int WIDTH_PADDING_CHARS = 6;

    private TabularExporter() {
    }

    public static byte[] toXlsx(String sheetName, String[] headers, List<String[]> rows) {
        String safeName = sheetName == null || sheetName.isBlank()
                ? "Sheet1"
                : sheetName.substring(0, Math.min(31, sheetName.length()));

        try (SXSSFWorkbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.setCompressTempFiles(true);
            Sheet sh = wb.createSheet(safeName);
            if (sh instanceof SXSSFSheet sxssfSheet) {
                sxssfSheet.trackAllColumnsForAutoSizing();
            }
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle bodyStyle = createBodyStyle(wb);

            int r = 0;
            Row headerRow = sh.createRow(r++);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c] == null ? "" : headers[c]);
                cell.setCellStyle(headerStyle);
            }
            for (String[] row : rows) {
                Row dataRow = sh.createRow(r++);
                for (int c = 0; c < headers.length; c++) {
                    String v = c < row.length && row[c] != null ? row[c] : "";
                    Cell cell = dataRow.createCell(c);
                    cell.setCellValue(v);
                    cell.setCellStyle(bodyStyle);
                }
            }

            // 固定表頭，捲動時仍可辨識欄位。
            sh.createFreezePane(0, 1);
            adjustColumns(sh, headers, rows);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("無法產生 Excel 檔", e);
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(LIANHUA_GREEN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static void adjustColumns(Sheet sh, String[] headers, List<String[]> rows) {
        int rowCount = rows == null ? 0 : rows.size();
        if (rowCount <= AUTO_SIZE_ROW_THRESHOLD) {
            autoSizeColumns(sh, headers, rows);
            return;
        }
        fallbackColumnWidth(sh, headers, rows);
    }

    /**
     * 粗略估算儲存格在 Excel 中的「顯示寬度」：CJK 等全形字元權重 2，ASCII 數字等權重 1。
     * （POI autoSize 對中文常低估，需手動補足。）
     */
    private static int estimateDisplayWidthUnits(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int units = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (isWideChar(ch)) {
                units += 2;
            } else {
                units += 1;
            }
        }
        return units;
    }

    private static boolean isWideChar(char ch) {
        if (ch >= 0x4E00 && ch <= 0x9FFF) {
            return true;
        }
        if (ch >= 0x3400 && ch <= 0x4DBF) {
            return true;
        }
        if (ch >= 0xAC00 && ch <= 0xD7AF) {
            return true;
        }
        if (ch >= 0x3000 && ch <= 0x303F) {
            return true;
        }
        return ch > 0x00FF;
    }

    private static int maxContentDisplayUnits(String[] headers, List<String[]> rows, int col) {
        String h = headers != null && col < headers.length ? headers[col] : "";
        int max = estimateDisplayWidthUnits(h);
        if (rows == null || rows.isEmpty()) {
            return max;
        }
        int limit = Math.min(rows.size(), WIDTH_SAMPLE_ROW_CAP);
        for (int r = 0; r < limit; r++) {
            String[] row = rows.get(r);
            String v = row != null && col < row.length && row[col] != null ? row[col] : "";
            max = Math.max(max, estimateDisplayWidthUnits(v));
        }
        return max;
    }

    private static int toPoiWidthChars(int displayUnits) {
        int chars = displayUnits + WIDTH_PADDING_CHARS;
        chars = Math.max(chars, MIN_WIDTH_CHARS_FLOOR);
        // 長字串（如「合計 (yyyy-MM-dd ~ yyyy-MM-dd)」）需要較高上限
        chars = Math.min(chars, 100);
        return chars * 256;
    }

    private static void autoSizeColumns(Sheet sh, String[] headers, List<String[]> rows) {
        Objects.requireNonNull(rows, "rows");
        int columnCount = headers == null ? 0 : headers.length;
        for (int c = 0; c < columnCount; c++) {
            sh.autoSizeColumn(c);
            int autoBased = sh.getColumnWidth(c);

            int contentUnits = maxContentDisplayUnits(headers, rows, c);
            int minFromContent = toPoiWidthChars(contentUnits);

            // 在 autoSize 結果上再加寬，並以「表頭＋資料」估算為下限
            int padded = autoBased + 8 * 256;
            int width = Math.max(padded, minFromContent);
            sh.setColumnWidth(c, Math.min(width, MAX_COL_WIDTH_UNITS));
        }
    }

    private static void fallbackColumnWidth(Sheet sh, String[] headers, List<String[]> rows) {
        int columnCount = headers == null ? 0 : headers.length;
        for (int c = 0; c < columnCount; c++) {
            int contentUnits = maxContentDisplayUnits(headers, rows, c);
            int width = Math.min(toPoiWidthChars(contentUnits), MAX_COL_WIDTH_UNITS);
            sh.setColumnWidth(c, width);
        }
    }

    /**
     * UTF-8 含 BOM，便於 Excel 直接開啟中文欄位。
     */
    public static byte[] toCsvUtf8Bom(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(line(headers)).append('\n');
        for (String[] row : rows) {
            sb.append(line(row, headers.length)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String line(String[] cells) {
        return line(cells, cells.length);
    }

    private static String line(String[] cells, int len) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                line.append(',');
            }
            String v = i < cells.length && cells[i] != null ? cells[i] : "";
            line.append(escapeCsvField(v));
        }
        return line.toString();
    }

    private static String escapeCsvField(String v) {
        String safe = sanitizeCsvFormula(v);
        if (safe.isEmpty()) {
            return safe;
        }
        boolean needQuote = safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0;
        String escaped = safe.replace("\"", "\"\"");
        if (needQuote) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String sanitizeCsvFormula(String v) {
        if (v.isEmpty()) {
            return v;
        }
        char first = v.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + v;
        }
        return v;
    }
}
