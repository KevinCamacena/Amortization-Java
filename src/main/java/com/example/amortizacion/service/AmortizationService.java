package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.ChartPoint;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AmortizationService {

    public double calculateMonthlyPayment(double principal, double annualRate, int months) {
        if (principal <= 0 || months <= 0) {
            return 0.0;
        }
        if (annualRate == 0) {
            return principal / months;
        }
        double monthlyRate = annualRate / 100.0 / 12;
        return principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
    }

    public List<AmortizationRow> generateSchedule(Loan loan, boolean includeExtraPayments) {
        double principal = loan.getPrincipal() != null ? loan.getPrincipal() : 0.0;
        double annualRate = loan.getAnnualInterestRate() != null ? loan.getAnnualInterestRate() : 0.0;
        int months = loan.getTermMonths() != null ? loan.getTermMonths() : 0;

        if (principal <= 0 || months <= 0) {
            return List.of();
        }

        double monthlyRate = annualRate / 100.0 / 12;
        double monthlyPayment = calculateMonthlyPayment(principal, annualRate, months);

        double balance = principal;
        double totalInterest = 0;
        double totalPrincipal = 0;

        List<ExtraPayment> extraPayments = (includeExtraPayments && loan.getExtraPayments() != null)
            ? loan.getExtraPayments().stream()
                .filter(ep -> Boolean.TRUE.equals(ep.getEnabled()))
                .toList()
            : List.of();

        List<AmortizationRow> schedule = new ArrayList<>();

        for (int month = 1; month <= months; month++) {
            double interestPayment = balance * monthlyRate;
            double scheduledPrincipalPayment = monthlyPayment - interestPayment;

            double extra = 0;
            for (ExtraPayment ep : extraPayments) {
                if (ep.appliesToMonth(month)) {
                    extra += ep.getAmount();
                }
            }

            double totalPrincipalPayment = scheduledPrincipalPayment + extra;

            if (balance <= totalPrincipalPayment) {
                totalPrincipalPayment = balance;
                balance = 0;
                totalInterest += interestPayment;
                totalPrincipal += totalPrincipalPayment;

                schedule.add(new AmortizationRow(
                    month,
                    totalPrincipalPayment + interestPayment,
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
                scheduledPrincipalPayment + extra + interestPayment,
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

    public PayoffComparison calculatePayoffComparison(Loan loan) {
        List<AmortizationRow> standardSchedule = generateSchedule(loan, false);
        List<AmortizationRow> acceleratedSchedule = generateSchedule(loan, true);

        int standardMonths = standardSchedule.isEmpty() ? 0 : standardSchedule.get(standardSchedule.size() - 1).month();
        double standardInterest = standardSchedule.isEmpty() ? 0 : standardSchedule.get(standardSchedule.size() - 1).totalInterestPaid();
        double standardTotalCost = (loan.getPrincipal() != null ? loan.getPrincipal() : 0.0) + standardInterest;

        int acceleratedMonths = acceleratedSchedule.isEmpty() ? 0 : acceleratedSchedule.get(acceleratedSchedule.size() - 1).month();
        double acceleratedInterest = acceleratedSchedule.isEmpty() ? 0 : acceleratedSchedule.get(acceleratedSchedule.size() - 1).totalInterestPaid();
        double acceleratedTotalCost = (loan.getPrincipal() != null ? loan.getPrincipal() : 0.0) + acceleratedInterest;

        int monthsSaved = Math.max(0, standardMonths - acceleratedMonths);
        double interestSaved = Math.max(0, standardInterest - acceleratedInterest);

        boolean hasActiveExtraPayments = loan.getExtraPayments() != null &&
                loan.getExtraPayments().stream().anyMatch(ep -> Boolean.TRUE.equals(ep.getEnabled()));

        double totalExtra = 0.0;
        if (hasActiveExtraPayments) {
            double scheduledMonthlyPayment = calculateMonthlyPayment(
                    loan.getPrincipal() != null ? loan.getPrincipal() : 0.0,
                    loan.getAnnualInterestRate() != null ? loan.getAnnualInterestRate() : 0.0,
                    loan.getTermMonths() != null ? loan.getTermMonths() : 0
            );
            for (AmortizationRow row : acceleratedSchedule) {
                double scheduledP = scheduledMonthlyPayment - row.interestPaid();
                if (row.principalPaid() > scheduledP + 0.01) {
                    totalExtra += (row.principalPaid() - scheduledP);
                }
            }
        }

        return new PayoffComparison(
            standardMonths,
            acceleratedMonths,
            monthsSaved,
            standardInterest,
            acceleratedInterest,
            interestSaved,
            standardTotalCost,
            acceleratedTotalCost,
            totalExtra,
            hasActiveExtraPayments
        );
    }

    public LoanCompareSummary generateCompareSummary(Loan loan, boolean includeExtraPayments) {
        List<AmortizationRow> schedule = generateSchedule(loan, includeExtraPayments);
        AmortizationRow last = schedule.isEmpty() ? null : schedule.get(schedule.size() - 1);

        double principal = loan.getPrincipal() != null ? loan.getPrincipal() : 0.0;
        double annualRate = loan.getAnnualInterestRate() != null ? loan.getAnnualInterestRate() : 0.0;
        int termMonths = loan.getTermMonths() != null ? loan.getTermMonths() : 0;

        double monthlyPayment = calculateMonthlyPayment(principal, annualRate, termMonths);
        double totalInterest = last != null ? last.totalInterestPaid() : 0;
        int monthsToPayoff = last != null ? last.month() : termMonths;
        double totalCost = principal + totalInterest;

        List<ChartPoint> chartData = generateChartData(loan, includeExtraPayments);

        return new LoanCompareSummary(
            loan.getName(),
            loan.getTotalPrice(),
            loan.getDownPayment(),
            principal,
            monthlyPayment,
            totalInterest,
            monthsToPayoff,
            totalCost,
            chartData
        );
    }

    public record LoanCompareSummary(
        String name,
        Double totalPrice,
        Double downPayment,
        double principal,
        double monthlyPayment,
        double totalInterest,
        int monthsToPayoff,
        double totalCost,
        List<ChartPoint> chartData
    ) {}
}
