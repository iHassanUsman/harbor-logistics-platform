package com.hassanusman.harbor.shipment.cache;

import com.hassanusman.harbor.shipment.domain.Shipment;

import java.util.Optional;

public interface TrackingCachePort {

    void put(Shipment shipment);

    Optional<TrackingView> get(String shipmentId);

    record TrackingView(String id, String customerId, String status, String origin, String destination, String reason) {
    }
}
