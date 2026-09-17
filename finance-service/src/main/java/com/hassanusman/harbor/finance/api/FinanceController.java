package com.hassanusman.harbor.finance.api;

import com.hassanusman.harbor.finance.domain.CreditAccount;
import com.hassanusman.harbor.finance.domain.CreditAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final CreditAccountRepository accounts;

    public FinanceController(CreditAccountRepository accounts) {
        this.accounts = accounts;
    }

    public record CreditView(String customerId, BigDecimal creditLimit, BigDecimal used, BigDecimal available) {
        static CreditView from(CreditAccount a) {
            return new CreditView(a.getCustomerId(), a.getCreditLimit(), a.getUsed(), a.available());
        }
    }

    @GetMapping("/credits/{customerId}")
    public CreditView get(@PathVariable String customerId) {
        return accounts.findById(customerId)
                .map(CreditView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No credit account"));
    }
}
