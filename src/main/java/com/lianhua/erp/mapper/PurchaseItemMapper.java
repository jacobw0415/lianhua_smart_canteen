package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.PurchaseItem;
import com.lianhua.erp.dto.purchase.PurchaseItemDto;
import com.lianhua.erp.dto.purchase.PurchaseItemRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseItemMapper {

  @Mapping(target = "purchase", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "subtotal", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  PurchaseItem toEntity(PurchaseItemRequestDto dto);

  PurchaseItemDto toDto(PurchaseItem entity);
}
