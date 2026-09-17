package com.hassanusman.harbor.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hassanusman.harbor.shipment",
        "com.hassanusman.harbor.outbox",
        "com.hassanusman.harbor.web"
})
@EntityScan({"com.hassanusman.harbor.shipment", "com.hassanusman.harbor.outbox"})
@EnableJpaRepositories({"com.hassanusman.harbor.shipment", "com.hassanusman.harbor.outbox"})
@EnableFeignClients
@EnableScheduling
public class ShipmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShipmentApplication.class, args);
    }
}
