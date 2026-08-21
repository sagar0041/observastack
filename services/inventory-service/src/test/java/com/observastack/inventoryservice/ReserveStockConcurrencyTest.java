package com.observastack.inventoryservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.observastack.inventoryservice.api.dto.CreateStockItemRequest;
import com.observastack.inventoryservice.api.dto.ReservationResponse;
import com.observastack.inventoryservice.api.dto.ReserveStockRequest;
import com.observastack.inventoryservice.api.dto.StockItemResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The actual proof of this milestone: fires more concurrent reservation
 * requests against one SKU than it has stock for, over real HTTP against
 * a real Postgres, and checks that the ledger comes out exactly right —
 * not "probably right," the exact count. A test that only reserved stock
 * from one thread at a time wouldn't exercise the optimistic-locking
 * path this milestone exists for at all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReserveStockConcurrencyTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reserve_neverOversells_underConcurrentContention()
            throws InterruptedException, ExecutionException, TimeoutException {
        String sku = "CONTESTED-" + UUID.randomUUID();
        int availableStock = 10;
        int concurrentRequests = 25;

        restTemplate.postForEntity("/stock-items", new CreateStockItemRequest(sku, availableStock), StockItemResponse.class);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        // Every thread blocks here until every other thread has also
        // reached this point, then they're all released together — the
        // point is maximum simultaneous contention on one row, not just
        // "several requests at roughly the same time."
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<HttpStatusCode>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                UUID orderId = UUID.randomUUID();
                var request = new ReserveStockRequest(orderId, List.of(new ReserveStockRequest.LineRequest(sku, 1)));
                allThreadsReady.countDown();
                go.await();
                ResponseEntity<ReservationResponse> response =
                        restTemplate.postForEntity("/reservations", request, ReservationResponse.class);
                return response.getStatusCode();
            }));
        }

        assertThat(allThreadsReady.await(10, TimeUnit.SECONDS)).as("every thread reached the starting line").isTrue();
        go.countDown();

        List<HttpStatusCode> results = new ArrayList<>();
        for (Future<HttpStatusCode> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long succeeded = results.stream().filter(status -> status == HttpStatus.CREATED).count();
        long rejected = results.stream().filter(status -> status == HttpStatus.CONFLICT).count();

        assertThat(succeeded + rejected)
                .as("every request got a definitive answer, none silently lost")
                .isEqualTo(concurrentRequests);
        assertThat(succeeded).as("exactly as many requests won as there was stock for").isEqualTo(availableStock);

        StockItemResponse finalState =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(finalState.availableQuantity())
                .as("stock ledger reconciles exactly, never oversold, never negative")
                .isZero();
    }
}
