package com.example.amortizacion.controller;

import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.entity.RecurrenceType;
import com.example.amortizacion.service.AmortizationService;
import com.example.amortizacion.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanViewControllerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private AmortizationService amortizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoanViewController controller = new LoanViewController(loanService, amortizationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createWithCarCostAndDownPaymentCalculatesPrincipal() throws Exception {
        when(loanService.create(any(Loan.class))).thenAnswer(invocation -> {
            Loan l = invocation.getArgument(0);
            l.setId(10L);
            return l;
        });

        mockMvc.perform(post("/loans")
                .param("name", "Subaru Outback")
                .param("totalPrice", "35000")
                .param("downPaymentPercent", "20")
                .param("annualInterestRate", "5.5")
                .param("termMonths", "60"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loans/10"));

        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanService).create(captor.capture());
        Loan captured = captor.getValue();

        assertThat(captured.getName()).isEqualTo("Subaru Outback");
        assertThat(captured.getTotalPrice()).isEqualTo(35000.0);
        assertThat(captured.getDownPayment()).isEqualTo(7000.0);
        assertThat(captured.getPrincipal()).isEqualTo(28000.0);
    }

    @Test
    void createWithRecurringExtraPaymentAddsPayment() throws Exception {
        when(loanService.create(any(Loan.class))).thenAnswer(invocation -> {
            Loan l = invocation.getArgument(0);
            l.setId(20L);
            return l;
        });

        mockMvc.perform(post("/loans")
                .param("name", "Auto Loan")
                .param("principal", "25000")
                .param("annualInterestRate", "6.0")
                .param("termMonths", "48")
                .param("extraPaymentMonth", "1")
                .param("extraPaymentAmount", "150")
                .param("extraPaymentRecurrence", "MONTHLY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loans/20"));

        ArgumentCaptor<ExtraPayment> epCaptor = ArgumentCaptor.forClass(ExtraPayment.class);
        verify(loanService).addExtraPayment(eq(20L), epCaptor.capture());
        ExtraPayment ep = epCaptor.getValue();

        assertThat(ep.getAmount()).isEqualTo(150.0);
        assertThat(ep.getMonthNumber()).isEqualTo(1);
        assertThat(ep.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
    }

    @Test
    void addExtraPaymentSupportsAnnualRecurrence() throws Exception {
        mockMvc.perform(post("/loans/15/extra-payment")
                .param("monthNumber", "12")
                .param("endMonth", "60")
                .param("amount", "1000")
                .param("recurrenceType", "ANNUALLY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loans/15"));

        ArgumentCaptor<ExtraPayment> epCaptor = ArgumentCaptor.forClass(ExtraPayment.class);
        verify(loanService).addExtraPayment(eq(15L), epCaptor.capture());
        ExtraPayment ep = epCaptor.getValue();

        assertThat(ep.getAmount()).isEqualTo(1000.0);
        assertThat(ep.getMonthNumber()).isEqualTo(12);
        assertThat(ep.getEndMonth()).isEqualTo(60);
        assertThat(ep.getRecurrenceType()).isEqualTo(RecurrenceType.ANNUALLY);
    }
}
