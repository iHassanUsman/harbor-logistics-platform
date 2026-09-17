package com.hassanusman.harbor.shipment.api;

import com.hassanusman.harbor.shipment.cache.TrackingCachePort;
import com.hassanusman.harbor.shipment.domain.BookingService;
import com.hassanusman.harbor.shipment.domain.Shipment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final BookingService bookings;
    private final TrackingCachePort cache;

    public ShipmentController(BookingService bookings, TrackingCachePort cache) {
        this.bookings = bookings;
        this.cache = cache;
    }

    public record BookRequest(
            @NotBlank String customerId,
            @NotBlank String origin,
            @NotBlank String destination,
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record ShipmentView(String id, String customerId, String origin, String destination,
                               BigDecimal amount, String status, String reason) {
        static ShipmentView from(Shipment s) {
            return new ShipmentView(s.getId(), s.getCustomerId(), s.getOrigin(), s.getDestination(),
                    s.getAmount(), s.getStatus().name(), s.getRejectReason());
        }
    }

    @PostMapping
    public ShipmentView book(@Valid @RequestBody BookRequest request) {
        return ShipmentView.from(bookings.book(request.customerId(), request.origin(), request.destination(), request.amount()));
    }

    @GetMapping("/{id}")
    public ShipmentView get(@PathVariable String id) {
        return cache.get(id)
                .map(v -> new ShipmentView(v.id(), v.customerId(), v.origin(), v.destination(), null, v.status(), v.reason()))
                .orElseGet(() -> ShipmentView.from(bookings.require(id)));
    }
}
