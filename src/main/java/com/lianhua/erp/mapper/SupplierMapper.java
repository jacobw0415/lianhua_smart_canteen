package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Supplier;
import com.lianhua.erp.dto.supplier.SupplierResponseDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE // ⭐ 阻止 null 覆蓋原資料
)
public interface SupplierMapper {

    SupplierResponseDto toDto(Supplier supplier);

    Supplier toEntity(SupplierRequestDto dto);

    void updateEntityFromDto(SupplierRequestDto dto, @MappingTarget Supplier supplier);

    List<SupplierResponseDto> toDtoList(List<Supplier> suppliers);
}
