package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @GetMapping
    public List<PaymentResponseDto> findAll() {
        return paymentService.findAll();
    }
    
    @GetMapping("/{purchaseId}")
    public PaymentResponseDto findByPurchase(@PathVariable Long purchaseId) {
        return paymentService.findByPurchaseId(purchaseId);
    }
    
    @DeleteMapping("/{purchaseId}")
    public void deleteByPurchase(@PathVariable Long purchaseId) {
        paymentService.deleteByPurchaseId(purchaseId);
    }
}
