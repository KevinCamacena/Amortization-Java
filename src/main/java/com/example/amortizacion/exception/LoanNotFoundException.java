package com.example.amortizacion.exception;

public class LoanNotFoundException extends RuntimeException {

    private final Long id;

    public LoanNotFoundException(Long id) {
        super("Loan not found with id: " + id);
        this.id = id;
    }

    public Long getId() { return id; }
}
