package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.entity.RecurrenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AmortizationServiceTest {

    private AmortizationService service;

    @BeforeEach
    void setUp() {
        service = new AmortizationService();
    }

    @Test
    void standardScheduleWithoutExtraPayments() {
        Loan loan = new Loan();
        loan.setName("Standard Auto Loan");
        loan.setPrincipal(20000.0);
        loan.setAnnualInterestRate(6.0);
        loan.setTermMonths(48);

        List<AmortizationRow> schedule = service.generateSchedule(loan, false);

        assertThat(schedule).hasSize(48);
        AmortizationRow last = schedule.get(schedule.size() - 1);
        assertThat(last.remainingBalance()).isEqualTo(0.0);
        assertThat(last.totalPrincipalPaid()).isCloseTo(20000.0, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void monthlyRecurringExtraPaymentsAcceleratesPayoff() {
        Loan loan = new Loan();
        loan.setName("Car Loan with Extra Payments");
        loan.setPrincipal(20000.0);
        loan.setAnnualInterestRate(6.0);
        loan.setTermMonths(48);

        // Add $200 recurring extra payment every month starting from month 1
        ExtraPayment recurring = new ExtraPayment(1, 200.0, RecurrenceType.MONTHLY);
        loan.addExtraPayment(recurring);

        List<AmortizationRow> standard = service.generateSchedule(loan, false);
        List<AmortizationRow> accelerated = service.generateSchedule(loan, true);

        // Should pay off much faster than 48 months
        assertThat(accelerated.size()).isLessThan(standard.size());

        PayoffComparison comparison = service.calculatePayoffComparison(loan);
        assertThat(comparison.monthsSaved()).isGreaterThan(0);
        assertThat(comparison.interestSaved()).isGreaterThan(0);
        assertThat(comparison.hasActiveExtraPayments()).isTrue();
    }

    @Test
    void annualRecurringExtraPaymentAppliesOncePerYear() {
        Loan loan = new Loan();
        loan.setName("Mortgage");
        loan.setPrincipal(100000.0);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(120);

        // Add $5000 once a year at month 12
        ExtraPayment annualExtra = new ExtraPayment(12, 5000.0, RecurrenceType.ANNUALLY);
        loan.addExtraPayment(annualExtra);

        List<AmortizationRow> accelerated = service.generateSchedule(loan, true);

        // Month 12 and Month 24 should have high principal paid
        AmortizationRow m12 = accelerated.stream().filter(r -> r.month() == 12).findFirst().orElseThrow();
        AmortizationRow m11 = accelerated.stream().filter(r -> r.month() == 11).findFirst().orElseThrow();
        assertThat(m12.principalPaid()).isGreaterThan(m11.principalPaid() + 4900.0);

        PayoffComparison comparison = service.calculatePayoffComparison(loan);
        assertThat(comparison.monthsSaved()).isGreaterThan(0);
        assertThat(comparison.interestSaved()).isGreaterThan(0);
    }
}

