package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Supplier;
import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplierMapper {
    
    SupplierDto toDto(Supplier supplier);
    
    Supplier toEntity(SupplierRequestDto dto);
    
    void updateEntityFromDto(SupplierRequestDto dto, @MappingTarget Supplier supplier);
}
