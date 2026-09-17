package com.hassanusman.harbor.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.hassanusman.harbor.customer",
        "com.hassanusman.harbor.web"
})
@EntityScan("com.hassanusman.harbor.customer")
@EnableJpaRepositories("com.hassanusman.harbor.customer")
public class CustomerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
