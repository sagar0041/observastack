package com.observastack.inventoryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises {@link StockItemRepositoryImpl} against a real PostgreSQL
 * instance, migrated by the service's actual Liquibase changelog.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StockItemRepositoryImplTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired
    private StockItemJpaRepository jpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private StockItemRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StockItemRepositoryImpl(jpaRepository, new StockItemMapper());
    }

    @Test
    void save_thenFindBySku_returnsAnEquivalentStockItem() {
        Sku sku = new Sku("WIDGET-1");

        repository.save(StockItem.of(sku, 10));
        Optional<StockItem> found = repository.findBySku(sku);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().availableQuantity()).isEqualTo(10);
    }

    @Test
    void findBySku_returnsEmpty_whenStockItemDoesNotExist() {
        assertThat(repository.findBySku(new Sku("NO-SUCH-SKU"))).isEmpty();
    }

    @Test
    void update_persistsChangedQuantity() {
        Sku sku = new Sku("WIDGET-1");
        repository.save(StockItem.of(sku, 10));
        StockItem item = repository.findBySku(sku).orElseThrow();

        item.reserve(4);
        repository.update(item);

        assertThat(repository.findBySku(sku).orElseThrow().availableQuantity()).isEqualTo(6);
    }

    @Test
    void update_throwsConcurrentStockUpdate_whenRowChangedSinceRead() {
        Sku sku = new Sku("WIDGET-1");
        repository.save(StockItem.of(sku, 10));
        StockItem staleRead = repository.findBySku(sku).orElseThrow();
        staleRead.reserve(2);

        // Simulate another transaction having already committed a change
        // to this row. A native update bypasses Hibernate's first-level
        // cache, so the entity this test's persistence context is
        // already tracking (from findBySku above) stays unaware of it —
        // exactly the staleness @Version exists to catch.
        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE stock_items SET available_quantity = 5, version = version + 1 WHERE sku = :sku")
                .setParameter("sku", sku.value())
                .executeUpdate();

        assertThatExceptionOfType(ConcurrentStockUpdateException.class).isThrownBy(() -> repository.update(staleRead));
    }
}
