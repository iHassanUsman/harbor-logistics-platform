package com.hassanusman.harbor.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Stand-in for JWT validation at the edge. Production would verify RS256 against the IdP JWKS.
 */
@Component
public class BearerStubFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            return chain.filter(exchange);
        }
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange.mutate()
                .request(builder -> builder.header("X-Correlation-Id",
                        exchange.getRequest().getHeaders().getFirst("X-Correlation-Id") == null
                                ? java.util.UUID.randomUUID().toString()
                                : exchange.getRequest().getHeaders().getFirst("X-Correlation-Id")))
                .build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
