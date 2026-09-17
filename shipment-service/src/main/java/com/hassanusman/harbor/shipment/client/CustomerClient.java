package com.hassanusman.harbor.shipment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "customer-service", url = "${harbor.customer-base-url:http://localhost:8081}")
public interface CustomerClient {

    record CustomerView(String id, String name, String type, BigDecimal creditLimit) {
    }

    @GetMapping("/api/customers/{id}")
    CustomerView get(@PathVariable("id") String id);
}
