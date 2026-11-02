package com.example.amortizacion.dto;

public record PayoffComparison(
    int standardMonths,
    int acceleratedMonths,
    int monthsSaved,
    double standardTotalInterest,
    double acceleratedTotalInterest,
    double interestSaved,
    double standardTotalCost,
    double acceleratedTotalCost,
    double totalExtraPaid,
    boolean hasActiveExtraPayments
) {}

