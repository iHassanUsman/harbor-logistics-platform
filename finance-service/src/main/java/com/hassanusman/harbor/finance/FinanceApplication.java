package com.hassanusman.harbor.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hassanusman.harbor.finance",
        "com.hassanusman.harbor.outbox",
        "com.hassanusman.harbor.web"
})
@EntityScan({"com.hassanusman.harbor.finance", "com.hassanusman.harbor.outbox"})
@EnableJpaRepositories({"com.hassanusman.harbor.finance", "com.hassanusman.harbor.outbox"})
@EnableScheduling
public class FinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
