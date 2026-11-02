package com.example.amortizacion.dto;

public record AmortizationRow(
    int month,
    double payment,
    double principalPaid,
    double interestPaid,
    double totalPrincipalPaid,
    double totalInterestPaid,
    double remainingBalance
) {}
