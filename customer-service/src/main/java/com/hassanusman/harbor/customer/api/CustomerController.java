package com.hassanusman.harbor.customer.api;

import com.hassanusman.harbor.customer.domain.Customer;
import com.hassanusman.harbor.customer.domain.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    public record CustomerView(String id, String name, String type, BigDecimal creditLimit) {
        static CustomerView from(Customer c) {
            return new CustomerView(c.getId(), c.getName(), c.getType(), c.getCreditLimit());
        }
    }

    @GetMapping
    public List<CustomerView> list() {
        return repository.findAll().stream().map(CustomerView::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerView get(@PathVariable String id) {
        return repository.findById(id)
                .map(CustomerView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
