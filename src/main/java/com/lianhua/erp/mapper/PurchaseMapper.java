package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.PurchaseDto;
import com.lianhua.erp.dto.PaymentDto;
import com.lianhua.erp.domin.Purchase;
import com.lianhua.erp.domin.Payment;
import com.lianhua.erp.domin.Supplier;
import com.lianhua.erp.repository.SupplierRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PurchaseMapper {

    private final SupplierRepository supplierRepository;
    private final PaymentMapper paymentMapper;

    public PurchaseMapper(SupplierRepository supplierRepository, PaymentMapper paymentMapper) {
        this.supplierRepository = supplierRepository;
        this.paymentMapper = paymentMapper;
    }

    public PurchaseDto toDto(Purchase purchase) {
        if (purchase == null) return null;

        List<PaymentDto> paymentDtos = purchase.getPayments() != null
                ? purchase.getPayments().stream().map(paymentMapper::toDto).collect(Collectors.toList())
                : null;

        return PurchaseDto.builder()
                .id(purchase.getId())
                .supplierId(purchase.getSupplier() != null ? purchase.getSupplier().getId() : null)
                .purchaseDate(purchase.getPurchaseDate() != null ? purchase.getPurchaseDate().toString() : null)
                .item(purchase.getItem())
                .qty(purchase.getQty())
                .unitPrice(purchase.getUnitPrice())
                .tax(purchase.getTax())
                .status(purchase.getStatus() != null ? purchase.getStatus().name() : null)
                .payments(paymentDtos)
                .build();
    }

    public Purchase toEntity(PurchaseDto dto) {
        if (dto == null) return null;

        Purchase purchase = new Purchase();
        updateEntityFromDto(dto, purchase);
        return purchase;
    }

    public void updateEntityFromDto(PurchaseDto dto, Purchase purchase) {
        if (dto == null || purchase == null) return;

        purchase.setId(dto.getId());

        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found: " + dto.getSupplierId()));
            purchase.setSupplier(supplier);
        }

        if (dto.getPurchaseDate() != null) {
            purchase.setPurchaseDate(java.time.LocalDate.parse(dto.getPurchaseDate()));
        }

        purchase.setItem(dto.getItem());
        purchase.setQty(dto.getQty());
        purchase.setUnitPrice(dto.getUnitPrice());
        purchase.setTax(dto.getTax());

        if (dto.getStatus() != null) {
            purchase.setStatus(Purchase.Status.valueOf(dto.getStatus()));
        }

        // payments 如果有帶進來，就一起處理（可選）
        if (dto.getPayments() != null) {
            List<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> p.setPurchase(purchase))
                    .collect(Collectors.toList());
            purchase.setPayments(payments);
        }
    }
}
