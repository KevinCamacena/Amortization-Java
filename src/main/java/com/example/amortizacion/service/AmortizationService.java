package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.ChartPoint;
import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmortizationService {

    public double calculateMonthlyPayment(double principal, double annualRate, int months) {
        if (annualRate == 0) {
            return principal / months;
        }
        double monthlyRate = annualRate / 100.0 / 12;
        return principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
    }

    public List<AmortizationRow> generateSchedule(Loan loan, boolean includeExtraPayments) {
        double principal = loan.getPrincipal();
        double annualRate = loan.getAnnualInterestRate();
        int months = loan.getTermMonths();
        double monthlyRate = annualRate / 100.0 / 12;
        double monthlyPayment = calculateMonthlyPayment(principal, annualRate, months);

        double balance = principal;
        double totalInterest = 0;
        double totalPrincipal = 0;

        java.util.List<ExtraPayment> extraPayments = includeExtraPayments
            ? loan.getExtraPayments().stream()
                .filter(ExtraPayment::getEnabled)
                .sorted((e1, e2) -> e1.getMonthNumber().compareTo(e2.getMonthNumber()))
                .toList()
            : java.util.List.of();

        java.util.List<AmortizationRow> schedule = new java.util.ArrayList<>();

        for (int month = 1; month <= months; month++) {
            double interestPayment = balance * monthlyRate;
            double principalPayment = monthlyPayment - interestPayment;

            double extra = 0;
            int extraIndex = 0;
            while (extraIndex < extraPayments.size() && extraPayments.get(extraIndex).getMonthNumber() == month) {
                extra += extraPayments.get(extraIndex).getAmount();
                extraIndex++;
            }

            double totalPrincipalPayment = principalPayment + extra;

            if (balance <= totalPrincipalPayment) {
                totalPrincipalPayment = balance;
                balance = 0;
                totalInterest += interestPayment;
                totalPrincipal += totalPrincipalPayment;

                schedule.add(new AmortizationRow(
                    month,
                    monthlyPayment + extra,
                    totalPrincipalPayment,
                    interestPayment,
                    totalPrincipal,
                    totalInterest,
                    balance
                ));
                break;
            }

            balance -= totalPrincipalPayment;
            totalInterest += interestPayment;
            totalPrincipal += totalPrincipalPayment;

            schedule.add(new AmortizationRow(
                month,
                monthlyPayment + extra,
                totalPrincipalPayment,
                interestPayment,
                totalPrincipal,
                totalInterest,
                balance
            ));
        }

        return schedule;
    }

    public List<ChartPoint> generateChartData(Loan loan, boolean includeExtraPayments) {
        List<AmortizationRow> schedule = generateSchedule(loan, includeExtraPayments);

        return schedule.stream()
            .map(row -> new ChartPoint(
                row.month(),
                row.totalPrincipalPaid(),
                row.totalInterestPaid(),
                row.remainingBalance()
            ))
            .toList();
    }

    public LoanCompareSummary generateCompareSummary(Loan loan, boolean includeExtraPayments) {
        List<AmortizationRow> schedule = generateSchedule(loan, includeExtraPayments);
        AmortizationRow last = schedule.isEmpty() ? null : schedule.get(schedule.size() - 1);

        double monthlyPayment = calculateMonthlyPayment(loan.getPrincipal(), loan.getAnnualInterestRate(), loan.getTermMonths());
        double totalInterest = last != null ? last.totalInterestPaid() : 0;
        int monthsToPayoff = last != null ? last.month() : loan.getTermMonths();
        double totalCost = loan.getPrincipal() + totalInterest;

        List<ChartPoint> chartData = generateChartData(loan, includeExtraPayments);

        return new LoanCompareSummary(
            loan.getName(),
            monthlyPayment,
            totalInterest,
            monthsToPayoff,
            totalCost,
            chartData
        );
    }

    public record LoanCompareSummary(
        String name,
        double monthlyPayment,
        double totalInterest,
        int monthsToPayoff,
        double totalCost,
        List<ChartPoint> chartData
    ) {}
}
