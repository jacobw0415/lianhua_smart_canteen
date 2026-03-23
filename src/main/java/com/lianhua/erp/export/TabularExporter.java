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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 將表格式資料（表頭 + 每列字串陣列）寫成 xlsx 或 CSV。
 */
public final class TabularExporter {

    private static final short LIANHUA_GREEN = IndexedColors.GREEN.getIndex();

    private TabularExporter() {
    }

    public static byte[] toXlsx(String sheetName, String[] headers, List<String[]> rows) {
        String safeName = sheetName == null || sheetName.isBlank()
                ? "Sheet1"
                : sheetName.substring(0, Math.min(31, sheetName.length()));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(safeName);
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
            autoSizeColumns(sh, headers);

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

    private static void autoSizeColumns(Sheet sh, String[] headers) {
        int columnCount = headers == null ? 0 : headers.length;
        int maxWidth = 255 * 256; // Excel 欄寬上限（POI 單位：字元寬度 * 256）
        for (int c = 0; c < columnCount; c++) {
            sh.autoSizeColumn(c);
            int currentWidth = sh.getColumnWidth(c);

            // POI 單位為「字元寬度的 1/256」；中文/表頭通常需要更大的最小寬度
            int headerLen = headers[c] == null ? 0 : headers[c].length();
            int targetChars = Math.max(headerLen + 6, 12); // 最少給 12 字元
            int targetWidth = Math.min(targetChars * 256, 60 * 256); // 上限避免爆寬

            // 再額外加 padding，避免看起來仍偏窄
            int padded = currentWidth + 4 * 256;
            int width = Math.max(padded, targetWidth);
            sh.setColumnWidth(c, Math.min(width, maxWidth));
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
        boolean needQuote = v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        String escaped = v.replace("\"", "\"\"");
        if (needQuote) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
