package com.example.amortizacion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Loan name is required")
    @Column(nullable = false)
    private String name;

    @PositiveOrZero(message = "Total price must be zero or greater")
    @Column(name = "total_price")
    private Double totalPrice;

    @PositiveOrZero(message = "Down payment must be zero or greater")
    @Column(name = "down_payment")
    private Double downPayment;

    @PositiveOrZero(message = "Down payment percentage must be zero or greater")
    @Column(name = "down_payment_percent")
    private Double downPaymentPercent;

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be greater than zero")
    @Column(nullable = false)
    private Double principal;

    @NotNull(message = "Interest rate is required")
    @Positive(message = "Interest rate must be greater than zero")
    @Column(name = "annual_interest_rate", nullable = false)
    private Double annualInterestRate;

    @NotNull(message = "Loan term is required")
    @Positive(message = "Loan term must be greater than zero")
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtraPayment> extraPayments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        syncFinancingFields();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        syncFinancingFields();
        updatedAt = LocalDateTime.now();
    }

    public void syncFinancingFields() {
        if (totalPrice != null && totalPrice > 0) {
            if (downPayment != null && downPayment >= 0) {
                downPaymentPercent = Math.round((downPayment / totalPrice) * 10000.0) / 100.0;
            } else if (downPaymentPercent != null && downPaymentPercent >= 0) {
                downPayment = Math.round(totalPrice * (downPaymentPercent / 100.0) * 100.0) / 100.0;
            }
            if (principal == null || principal <= 0) {
                double dp = downPayment != null ? downPayment : 0.0;
                principal = Math.max(0.01, Math.round((totalPrice - dp) * 100.0) / 100.0);
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public Double getDownPayment() { return downPayment; }
    public void setDownPayment(Double downPayment) { this.downPayment = downPayment; }

    public Double getDownPaymentPercent() { return downPaymentPercent; }
    public void setDownPaymentPercent(Double downPaymentPercent) { this.downPaymentPercent = downPaymentPercent; }

    public Double getPrincipal() { return principal; }
    public void setPrincipal(Double principal) { this.principal = principal; }

    public Double getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(Double annualInterestRate) { this.annualInterestRate = annualInterestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public double getMonthlyPayment() {
        if (principal == null || termMonths == null || annualInterestRate == null || termMonths <= 0) {
            return 0.0;
        }
        if (annualInterestRate == 0) {
            return principal / termMonths;
        }
        double monthlyRate = annualInterestRate / 100.0 / 12;
        return principal * (monthlyRate * Math.pow(1 + monthlyRate, termMonths))
                / (Math.pow(1 + monthlyRate, termMonths) - 1);
    }

    public List<ExtraPayment> getExtraPayments() { return extraPayments; }
    public void setExtraPayments(List<ExtraPayment> extraPayments) { this.extraPayments = extraPayments; }

    public void addExtraPayment(ExtraPayment extraPayment) {
        extraPayments.add(extraPayment);
        extraPayment.setLoan(this);
    }

    public void removeExtraPayment(ExtraPayment extraPayment) {
        extraPayments.remove(extraPayment);
        extraPayment.setLoan(null);
    }
}
