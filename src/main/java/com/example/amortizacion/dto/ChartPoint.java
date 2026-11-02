package com.example.amortizacion.dto;

public record ChartPoint(
    int month,
    double principalPaid,
    double interestPaid,
    double remainingBalance
) {}
