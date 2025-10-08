package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Sale;
import com.lianhua.erp.dto.sale.SaleRequestDto;
import com.lianhua.erp.dto.sale.SaleResponseDto;
import com.lianhua.erp.mapper.SaleMapper;
import com.lianhua.erp.repository.SaleRepository;
import com.lianhua.erp.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    public SaleServiceImpl(SaleRepository saleRepository, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
    }

    @Override
    public List<SaleResponseDto> getAllSales() {
        return saleRepository.findAll().stream()
                .map(saleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public SaleResponseDto getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + id));
        return saleMapper.toResponseDto(sale);
    }

    @Override
    public SaleResponseDto createSale(SaleRequestDto dto) {
        Sale sale = saleMapper.toEntity(dto);
        return saleMapper.toResponseDto(saleRepository.save(sale));
    }

    @Override
    public SaleResponseDto updateSale(Long id, SaleRequestDto dto) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + id));
        saleMapper.updateEntityFromDto(dto, sale);
        return saleMapper.toResponseDto(saleRepository.save(sale));
    }

    @Override
    public void deleteSale(Long id) {
        saleRepository.deleteById(id);
    }
}
