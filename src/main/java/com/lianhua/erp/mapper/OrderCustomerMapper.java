package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.OrderCustomerDto;
import com.lianhua.erp.domin.OrderCustomer;
import org.springframework.stereotype.Component;

@Component
public class OrderCustomerMapper {

    public OrderCustomerDto toDto(OrderCustomer customer) {
        if (customer == null) return null;

        return OrderCustomerDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .contactPerson(customer.getContactPerson())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .billingCycle(String.valueOf(customer.getBillingCycle()))
                .note(customer.getNote())
                .build();
    }

    public OrderCustomer toEntity(OrderCustomerDto dto) {
        if (dto == null) return null;

        return OrderCustomer.builder()
                .id(dto.getId())
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .billingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()))
                .note(dto.getNote())
                .build();
    }

    public void updateEntityFromDto(OrderCustomerDto dto, OrderCustomer entity) {
        if (dto == null || entity == null) return;

        entity.setName(dto.getName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setBillingCycle(OrderCustomer.BillingCycle.valueOf(dto.getBillingCycle()));
        entity.setNote(dto.getNote());
    }
}
