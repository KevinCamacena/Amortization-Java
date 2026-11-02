package com.example.amortizacion.util;

import com.example.amortizacion.entity.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanUtils {

    public double calculateMonthlyPayment(double principal, double annualRate, int months) {
        if (annualRate == 0) {
            return principal / months;
        }
        double monthlyRate = annualRate / 100.0 / 12;
        return principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
    }

    public double calcPayment(Loan loan) {
        return calculateMonthlyPayment(loan.getPrincipal(), loan.getAnnualInterestRate(), loan.getTermMonths());
    }
}
