package com.observastack.orderservice.domain;

/**
 * Lifecycle states of an {@link Order}.
 *
 * <p>An order starts life as {@link #CREATED} the instant its invariants
 * are satisfied (at least one line item, a known customer) — this is a
 * construction-time state, not yet a committed business event. It
 * becomes {@link #PLACED} once the placement use case commits it: this
 * is the state downstream milestones react to, such as reserving stock
 * (M3) and publishing the {@code OrderPlaced} Kafka event (M8).
 * {@link #CANCELLED} is the terminal alternate path, reachable from
 * either state — for when placement can't be honoured (stock
 * unavailable, payment declined) or a customer backs out before either
 * of those checks even runs.
 *
 * <p>There is no transition out of {@link #CANCELLED}, and no transition
 * back to {@link #CREATED} from {@link #PLACED}: placement is a one-way
 * door.
 */
public enum OrderStatus {
    CREATED,
    PLACED,
    CANCELLED
}
