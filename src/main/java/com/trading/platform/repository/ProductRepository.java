package com.trading.platform.repository;

import com.trading.platform.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // General query for active products with pagination
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    // Advanced query for active products supporting fuzzy search, price range, and sorting/pagination
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchActiveProducts(
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    // Query active product detail
    Optional<Product> findByIdAndIsDeletedFalse(Long id);
}
