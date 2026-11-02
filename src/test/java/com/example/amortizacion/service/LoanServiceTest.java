package com.example.amortizacion.service;

import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.repository.ExtraPaymentRepository;
import com.example.amortizacion.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ExtraPaymentRepository extraPaymentRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, extraPaymentRepository);
    }

    @Test
    void createSavesLoan() {
        Loan loan = new Loan();
        loan.setName("Car Loan");
        loan.setTotalPrice(25000.0);
        loan.setDownPayment(5000.0);
        loan.setPrincipal(20000.0);
        loan.setAnnualInterestRate(6.0);
        loan.setTermMonths(48);

        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Loan saved = loanService.create(loan);

        assertThat(saved.getName()).isEqualTo("Car Loan");
        verify(loanRepository).save(loan);
    }

    @Test
    void updateCopiesCarCostAndDownPayment() {
        Loan existing = new Loan();
        existing.setId(1L);
        existing.setName("Old Loan");
        existing.setPrincipal(10000.0);
        existing.setAnnualInterestRate(5.0);
        existing.setTermMonths(36);

        Loan updatedData = new Loan();
        updatedData.setName("Mazda CX-5");
        updatedData.setTotalPrice(32000.0);
        updatedData.setDownPaymentPercent(15.0);
        updatedData.setDownPayment(4800.0);
        updatedData.setPrincipal(27200.0);
        updatedData.setAnnualInterestRate(4.9);
        updatedData.setTermMonths(60);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.update(1L, updatedData);

        assertThat(result.getName()).isEqualTo("Mazda CX-5");
        assertThat(result.getTotalPrice()).isEqualTo(32000.0);
        assertThat(result.getDownPayment()).isEqualTo(4800.0);
        assertThat(result.getDownPaymentPercent()).isEqualTo(15.0);
        assertThat(result.getPrincipal()).isEqualTo(27200.0);
    }
}

