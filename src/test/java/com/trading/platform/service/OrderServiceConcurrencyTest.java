package com.trading.platform.service;

import com.trading.platform.dto.OrderRequest;
import com.trading.platform.entity.Product;
import com.trading.platform.entity.Role;
import com.trading.platform.entity.User;
import com.trading.platform.exception.InsufficientStockException;
import com.trading.platform.repository.OrderRepository;
import com.trading.platform.repository.ProductRepository;
import com.trading.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev") // Runs against H2 Database
public class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long productId;
    private final String username = "concurrency_user";

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Save a test user
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // Save a test product with exactly 10 units in stock
        Product product = Product.builder()
                .name("Concurrency Product")
                .price(BigDecimal.valueOf(100.00))
                .stock(10)
                .isDeleted(false)
                .build();
        Product saved = productRepository.save(product);
        productId = saved.getId();
    }

    @Test
    void testConcurrentOrders_StockShouldNotBeNegativeAndNoOverselling() throws InterruptedException {
        int numberOfThreads = 15;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedDueToStockCount = new AtomicInteger(0);
        AtomicInteger failedDueToConflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            tasks.add(() -> {
                latch.await(); // Wait for the start signal
                try {
                    OrderRequest request = new OrderRequest();
                    request.setProductId(productId);
                    request.setQuantity(1);
                    orderService.placeOrder(request, username);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failedDueToStockCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException ex) {
                    failedDueToConflictCount.incrementAndGet();
                } catch (Exception ex) {
                    System.err.println("Unexpected exception: " + ex.getMessage());
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executorService.submit(task));
        }

        latch.countDown(); // Fire all requests concurrently
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        Product product = productRepository.findById(productId).orElseThrow();

        System.out.println("Successful orders: " + successCount.get());
        System.out.println("Failed due to stock: " + failedDueToStockCount.get());
        System.out.println("Failed due to conflict (after 3 attempts): " + failedDueToConflictCount.get());
        System.out.println("Final product stock: " + product.getStock());

        // Verifications:
        // 1. Stock must be exactly 0 (fully depleted)
        assertEquals(0, product.getStock(), "Stock should be completely depleted");
        // 2. Successful orders must be exactly 10 (matching starting stock)
        assertEquals(10, successCount.get(), "Should have exactly 10 successful orders");
        // 3. Order records in database must be exactly 10 (no overselling)
        assertEquals(10, orderRepository.count(), "Total order records should be exactly 10");
    }
}
