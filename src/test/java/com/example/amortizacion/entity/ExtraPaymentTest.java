package com.example.amortizacion.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraPaymentTest {

    @Test
    void oneTimePaymentAppliesOnlyAtSpecifiedMonth() {
        ExtraPayment ep = new ExtraPayment(12, 1000.0, RecurrenceType.ONE_TIME);

        assertThat(ep.appliesToMonth(11)).isFalse();
        assertThat(ep.appliesToMonth(12)).isTrue();
        assertThat(ep.appliesToMonth(13)).isFalse();
    }

    @Test
    void monthlyPaymentAppliesEveryMonthStartingAtStartMonth() {
        ExtraPayment ep = new ExtraPayment(6, 200.0, RecurrenceType.MONTHLY);

        assertThat(ep.appliesToMonth(5)).isFalse();
        assertThat(ep.appliesToMonth(6)).isTrue();
        assertThat(ep.appliesToMonth(7)).isTrue();
        assertThat(ep.appliesToMonth(36)).isTrue();
    }

    @Test
    void monthlyPaymentRespectsEndMonth() {
        ExtraPayment ep = new ExtraPayment(6, 200.0, RecurrenceType.MONTHLY);
        ep.setEndMonth(12);

        assertThat(ep.appliesToMonth(5)).isFalse();
        assertThat(ep.appliesToMonth(6)).isTrue();
        assertThat(ep.appliesToMonth(12)).isTrue();
        assertThat(ep.appliesToMonth(13)).isFalse();
    }

    @Test
    void annualPaymentAppliesEveryTwelveMonths() {
        ExtraPayment ep = new ExtraPayment(12, 500.0, RecurrenceType.ANNUALLY);

        assertThat(ep.appliesToMonth(11)).isFalse();
        assertThat(ep.appliesToMonth(12)).isTrue();
        assertThat(ep.appliesToMonth(23)).isFalse();
        assertThat(ep.appliesToMonth(24)).isTrue();
        assertThat(ep.appliesToMonth(36)).isTrue();
    }

    @Test
    void disabledPaymentNeverApplies() {
        ExtraPayment ep = new ExtraPayment(1, 100.0, RecurrenceType.MONTHLY);
        ep.setEnabled(false);

        assertThat(ep.appliesToMonth(1)).isFalse();
        assertThat(ep.appliesToMonth(5)).isFalse();
    }
}

