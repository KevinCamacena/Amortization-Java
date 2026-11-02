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

    @NotNull
    @Positive
    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
}
