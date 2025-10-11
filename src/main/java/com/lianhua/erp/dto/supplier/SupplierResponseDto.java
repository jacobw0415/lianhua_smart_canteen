package com.lianhua.erp.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "供應商回應物件")
public class SupplierResponseDto {
    private Long id;
    private String message;
}
