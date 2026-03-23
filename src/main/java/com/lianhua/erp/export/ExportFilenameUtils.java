package com.lianhua.erp.export;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExportFilenameUtils {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ExportFilenameUtils() {
    }

    public static String build(String resourcePrefix, ExportFormat format) {
        return resourcePrefix + "_export_" + LocalDateTime.now().format(TS) + "." + format.fileExtension();
    }
}
