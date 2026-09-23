package com.artevia.service;

import com.artevia.dto.ProductCreateRequest;
import com.artevia.dto.ProductDto;
import com.artevia.mapper.ProductMapper;
import com.artevia.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        var product = productMapper.toEntity(request);
        var saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }
}