package com.trading.platform.service;

import com.trading.platform.dto.OrderRequest;
import com.trading.platform.dto.OrderResponse;
import com.trading.platform.entity.Order;
import com.trading.platform.entity.OrderStatus;
import com.trading.platform.entity.Product;
import com.trading.platform.entity.User;
import com.trading.platform.exception.InsufficientStockException;
import com.trading.platform.exception.ResourceNotFoundException;
import com.trading.platform.repository.OrderRepository;
import com.trading.platform.repository.ProductRepository;
import com.trading.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    @Lazy
    private OrderService self;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public OrderResponse placeOrder(OrderRequest request, String username) {
        int maxRetries = 8;
        int attempt = 0;

        while (true) {
            try {
                attempt++;
                // Call executePlaceOrder through the Spring proxy to start a new transaction
                return self.executePlaceOrder(request, username);
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= maxRetries) {
                    throw new ObjectOptimisticLockingFailureException(
                            "Failed to place order after " + maxRetries + " attempts due to concurrent updates.", 
                            ex
                    );
                }
                // Randomized sleep backoff to stagger retries and reduce thundering herd collisions
                try {
                    int backoff = java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 150);
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderResponse executePlaceOrder(OrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Product product = productRepository.findByIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or has been deleted"));

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() 
                    + " (requested: " + request.getQuantity() + ", available: " + product.getStock() + ")");
        }

        // Deduct stock, this updates the version field in PostgreSQL automatically via Hibernate's @Version
        product.setStock(product.getStock() - request.getQuantity());
        productRepository.save(product);

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .user(user)
                .product(product)
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.PAID)
                .build();

        Order savedOrder = orderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .username(order.getUser().getUsername())
                .productId(order.getProduct().getId())
                .productName(order.getProduct().getName())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
