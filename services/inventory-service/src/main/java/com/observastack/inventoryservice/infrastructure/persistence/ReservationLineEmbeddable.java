package com.observastack.inventoryservice.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Persisted representation of a domain {@code ReservationLine}.
 *
 * <p>An {@link Embeddable} element collection, not its own
 * {@code @Entity}: a reservation line has no identity independent of the
 * reservation it belongs to, matching the domain model.
 */
@Embeddable
public class ReservationLineEmbeddable {

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReservationLineEmbeddable() {
        // required by JPA
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
