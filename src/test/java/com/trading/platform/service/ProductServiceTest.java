package com.trading.platform.service;

import com.trading.platform.dto.ProductRequest;
import com.trading.platform.dto.ProductResponse;
import com.trading.platform.entity.Product;
import com.trading.platform.exception.ResourceNotFoundException;
import com.trading.platform.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProductService productService;

    private Product activeProduct;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(100.00))
                .stock(10)
                .isDeleted(false)
                .version(0L)
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Updated Name");
        productRequest.setPrice(BigDecimal.valueOf(150.00));
        productRequest.setStock(15);
    }

    @Test
    void testCreateProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals(activeProduct.getId(), response.getId());
        assertEquals(activeProduct.getName(), response.getName());
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditLogService, times(1)).log(eq("CREATE"), eq("Product"), eq(1L), any(), any());
    }

    @Test
    void testUpdateProduct_Success() {
        when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertNotNull(response);
        verify(productRepository, times(1)).findByIdAndIsDeletedFalse(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditLogService, times(1)).log(eq("UPDATE"), eq("Product"), eq(1L), any(), any());
    }

    @Test
    void testUpdateProduct_NotFound() {
        when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(1L, productRequest));
        verify(productRepository, times(0)).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_Success() {
        when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        productService.deleteProduct(1L);

        assertTrue(activeProduct.getIsDeleted());
        verify(productRepository, times(1)).save(activeProduct);
        verify(auditLogService, times(1)).log(eq("DELETE"), eq("Product"), eq(1L), any(), any());
    }

    @Test
    void testSearchProducts_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(activeProduct));
        when(productRepository.searchActiveProducts(anyString(), any(), any(), any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.searchProducts("Test", BigDecimal.ZERO, BigDecimal.valueOf(200), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(activeProduct.getName(), result.getContent().get(0).getName());
    }
}
