package com.hassanusman.harbor.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BearerStubFilterTest {

    private final BearerStubFilter filter = new BearerStubFilter();

    @Test
    void rejectsMissingBearer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/shipments").build());
        filter.filter(exchange, nextNeverCalled());
        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void allowsActuatorWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());
        boolean[] called = {false};
        filter.filter(exchange, e -> {
            called[0] = true;
            return Mono.empty();
        }).block();
        assertNull(exchange.getResponse().getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(called[0]);
    }

    @Test
    void allowsBearer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/customers/C1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer demo-token")
                        .build());
        boolean[] called = {false};
        filter.filter(exchange, e -> {
            called[0] = true;
            return Mono.empty();
        }).block();
        org.junit.jupiter.api.Assertions.assertTrue(called[0]);
    }

    private GatewayFilterChain nextNeverCalled() {
        return exchange -> {
            throw new AssertionError("chain should not run");
        };
    }
}
