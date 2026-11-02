package com.example.amortizacion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double principal;

    @NotNull
    @Positive
    @Column(name = "annual_interest_rate", nullable = false)
    private Double annualInterestRate;

    @NotNull
    @Positive
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
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrincipal() { return principal; }
    public void setPrincipal(Double principal) { this.principal = principal; }

    public Double getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(Double annualInterestRate) { this.annualInterestRate = annualInterestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public double getMonthlyPayment() {
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
