package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.SupplierDto;
import com.lianhua.erp.domin.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierDto toDto(Supplier supplier) {
        if (supplier == null) return null;

        return SupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contact(supplier.getContact())
                .phone(supplier.getPhone())
                .billingCycle(supplier.getBillingCycle() != null ? supplier.getBillingCycle().name() : null)
                .note(supplier.getNote())
                .build();
    }

    public Supplier toEntity(SupplierDto dto) {
        if (dto == null) return null;

        Supplier supplier = new Supplier();
        supplier.setId(dto.getId());
        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());

        if (dto.getBillingCycle() != null) {
            supplier.setBillingCycle(Supplier.BillingCycle.valueOf(dto.getBillingCycle()));
        }

        supplier.setNote(dto.getNote());
        return supplier;
    }
}

