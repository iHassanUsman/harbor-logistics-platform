package com.hassanusman.harbor.event;

public final class Topics {
    public static final String SHIPMENTS = "harbor.shipments";
    public static final String FINANCE = "harbor.finance";

    public static final String SHIPMENT_BOOKED = "ShipmentBooked";
    public static final String CREDIT_RESERVED = "CreditReserved";
    public static final String CREDIT_REJECTED = "CreditRejected";
    public static final String SHIPMENT_CONFIRMED = "ShipmentConfirmed";
    public static final String SHIPMENT_REJECTED = "ShipmentRejected";

    private Topics() {
    }
}
