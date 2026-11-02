package com.example.amortizacion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "extra_payments")
public class ExtraPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Starting month is required")
    @Positive(message = "Starting month must be positive")
    @Column(name = "month_number", nullable = false)
    private Integer monthNumber = 1;

    @Positive(message = "Ending month must be positive")
    @Column(name = "end_month")
    private Integer endMonth;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be greater than zero")
    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONE_TIME;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    public ExtraPayment() {}

    public ExtraPayment(Integer monthNumber, Double amount, RecurrenceType recurrenceType) {
        this.monthNumber = monthNumber;
        this.amount = amount;
        this.recurrenceType = recurrenceType != null ? recurrenceType : RecurrenceType.ONE_TIME;
        this.enabled = true;
    }

    public boolean appliesToMonth(int month) {
        if (!Boolean.TRUE.equals(enabled)) {
            return false;
        }
        int start = monthNumber != null ? monthNumber : 1;
        if (month < start) {
            return false;
        }
        if (endMonth != null && month > endMonth) {
            return false;
        }
        RecurrenceType type = recurrenceType != null ? recurrenceType : RecurrenceType.ONE_TIME;
        return switch (type) {
            case ONE_TIME -> month == start;
            case MONTHLY -> true;
            case ANNUALLY -> (month - start) % 12 == 0;
        };
    }

    public String getFormattedSchedule() {
        RecurrenceType type = recurrenceType != null ? recurrenceType : RecurrenceType.ONE_TIME;
        int start = monthNumber != null ? monthNumber : 1;
        return switch (type) {
            case ONE_TIME -> "Month " + start;
            case MONTHLY -> (endMonth != null)
                    ? "Months " + start + " - " + endMonth + " (Monthly)"
                    : (start == 1 ? "Every Month" : "From Month " + start + " (Monthly)");
            case ANNUALLY -> (endMonth != null)
                    ? "Months " + start + " - " + endMonth + " (Annually)"
                    : "Every 12 months (starts M" + start + ")";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public Integer getEndMonth() { return endMonth; }
    public void setEndMonth(Integer endMonth) { this.endMonth = endMonth; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
}
