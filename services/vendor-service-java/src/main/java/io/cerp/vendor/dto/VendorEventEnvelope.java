package io.cerp.vendor.dto;

import java.time.OffsetDateTime;

/**
 * Generic event envelope used for Kafka messages in the vendor domain. Structure: { type, timestamp, payload }
 */
public class VendorEventEnvelope<T> {

    private String type;
    private OffsetDateTime timestamp;
    private T payload;

    public VendorEventEnvelope() {
    }

    public VendorEventEnvelope(String type, OffsetDateTime timestamp, T payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
