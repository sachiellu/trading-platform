package com.trading.platform.service;

import com.trading.platform.dto.ProductRequest;
import com.trading.platform.dto.ProductResponse;
import com.trading.platform.entity.Product;
import com.trading.platform.exception.ResourceNotFoundException;
import com.trading.platform.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public ProductService(ProductRepository productRepository, AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .isDeleted(false)
                .build();

        Product saved = productRepository.save(product);

        // Audit Logging
        auditLogService.log("CREATE", "Product", saved.getId(), null, mapState(saved));

        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Map<String, Object> beforeState = mapState(product);

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        Product saved = productRepository.save(product);

        // Audit Logging
        auditLogService.log("UPDATE", "Product", saved.getId(), beforeState, mapState(saved));

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Map<String, Object> beforeState = mapState(product);

        product.setIsDeleted(true);
        Product saved = productRepository.save(product);

        // Audit Logging
        auditLogService.log("DELETE", "Product", saved.getId(), beforeState, mapState(saved));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.searchActiveProducts(name, minPrice, maxPrice, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .version(product.getVersion())
                .isDeleted(product.getIsDeleted())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Map<String, Object> mapState(Product product) {
        return Map.of(
                "name", product.getName(),
                "price", product.getPrice(),
                "stock", product.getStock(),
                "isDeleted", product.getIsDeleted()
        );
    }
}
