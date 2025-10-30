package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.domin.Receipt;
import com.lianhua.erp.dto.receipt.*;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface ReceiptMapper {
    
    /**
     * 將建立用 DTO 轉換為 Entity
     * 注意：不自動設置金額與訂單，Service 會補上。
     */
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "accountingPeriod", ignore = true)
    Receipt toEntity(ReceiptRequestDto dto);
    
    /**
     * 將 Entity 轉換為回應 DTO
     */
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "method", target = "method")
    ReceiptResponseDto toDto(Receipt entity);
    
    /**
     * 用於部分更新的映射：
     * - 忽略 null 欄位（不覆蓋現有值）
     * - 忽略不應修改的欄位（order、amount、accountingPeriod）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "order", ignore = true),
            @Mapping(target = "amount", ignore = true),
            @Mapping(target = "accountingPeriod", ignore = true)
    })
    void updateEntityFromDto(ReceiptRequestDto dto, @MappingTarget Receipt entity);
}
