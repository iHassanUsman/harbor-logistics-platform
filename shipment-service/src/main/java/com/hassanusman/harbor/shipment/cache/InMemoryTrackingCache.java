package com.hassanusman.harbor.shipment.cache;

import com.hassanusman.harbor.shipment.domain.Shipment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(TrackingCachePort.class)
public class InMemoryTrackingCache implements TrackingCachePort {

    private final Map<String, TrackingView> store = new ConcurrentHashMap<>();

    @Override
    public void put(Shipment shipment) {
        store.put(shipment.getId(), new TrackingView(
                shipment.getId(),
                shipment.getCustomerId(),
                shipment.getStatus().name(),
                shipment.getOrigin(),
                shipment.getDestination(),
                shipment.getRejectReason()));
    }

    @Override
    public Optional<TrackingView> get(String shipmentId) {
        return Optional.ofNullable(store.get(shipmentId));
    }
}
