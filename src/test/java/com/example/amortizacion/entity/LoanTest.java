package com.example.amortizacion.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LoanTest {

    @Test
    void syncFinancingFieldsCalculatesDownPaymentFromPercentage() {
        Loan loan = new Loan();
        loan.setName("Honda Civic");
        loan.setTotalPrice(30000.0);
        loan.setDownPaymentPercent(20.0);
        loan.setAnnualInterestRate(5.0);
        loan.setTermMonths(60);

        loan.syncFinancingFields();

        assertThat(loan.getDownPayment()).isEqualTo(6000.0);
        assertThat(loan.getPrincipal()).isEqualTo(24000.0);
        assertThat(loan.getDownPaymentPercent()).isEqualTo(20.0);
    }

    @Test
    void syncFinancingFieldsCalculatesPercentageFromDownPaymentAmount() {
        Loan loan = new Loan();
        loan.setName("Toyota RAV4");
        loan.setTotalPrice(40000.0);
        loan.setDownPayment(10000.0);
        loan.setAnnualInterestRate(4.5);
        loan.setTermMonths(48);

        loan.syncFinancingFields();

        assertThat(loan.getDownPaymentPercent()).isCloseTo(25.0, within(0.01));
        assertThat(loan.getPrincipal()).isEqualTo(30000.0);
    }

    @Test
    void syncFinancingFieldsRetainsDirectPrincipalWhenNoTotalPrice() {
        Loan loan = new Loan();
        loan.setName("Personal Loan");
        loan.setPrincipal(15000.0);
        loan.setAnnualInterestRate(7.0);
        loan.setTermMonths(36);

        loan.syncFinancingFields();

        assertThat(loan.getPrincipal()).isEqualTo(15000.0);
        assertThat(loan.getTotalPrice()).isNull();
        assertThat(loan.getDownPayment()).isNull();
    }
}

