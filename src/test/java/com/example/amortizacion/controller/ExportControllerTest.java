package com.example.amortizacion.controller;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.AmortizationService;
import com.example.amortizacion.service.ExportService;
import com.example.amortizacion.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.OutputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private AmortizationService amortizationService;

    @Mock
    private ExportService exportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExportController controller = new ExportController(loanService, amortizationService, exportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void exportPdfReturnsPdfResponseAndCallsService() throws Exception {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setName("Auto Loan 20%");
        loan.setPrincipal(25000.0);
        loan.setAnnualInterestRate(5.5);
        loan.setTermMonths(48);

        List<AmortizationRow> schedule = List.of(
            new AmortizationRow(1, 581.42, 466.84, 114.58, 466.84, 114.58, 24533.16)
        );
        PayoffComparison comparison = new PayoffComparison(48, 48, 0, 2908.16, 2908.16, 0.0, 27908.16, 27908.16, 0.0, false);

        when(loanService.findByIdOrThrow(1L)).thenReturn(loan);
        when(amortizationService.generateSchedule(loan, true)).thenReturn(schedule);
        when(amortizationService.calculatePayoffComparison(loan)).thenReturn(comparison);

        mockMvc.perform(get("/loans/1/export/pdf").param("includeExtra", "true"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_Auto_Loan_20_.pdf\""));

        verify(exportService).exportLoanToPdf(eq(loan), eq(schedule), eq(comparison), eq(true), any(OutputStream.class));
    }

    @Test
    void exportCsvReturnsCsvResponseAndCallsService() throws Exception {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setName("Auto Loan 20%");

        List<AmortizationRow> schedule = List.of(
            new AmortizationRow(1, 581.42, 466.84, 114.58, 466.84, 114.58, 24533.16)
        );

        when(loanService.findByIdOrThrow(1L)).thenReturn(loan);
        when(amortizationService.generateSchedule(loan, true)).thenReturn(schedule);

        mockMvc.perform(get("/loans/1/export/csv").param("includeExtra", "true"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_Auto_Loan_20_.csv\""));

        verify(exportService).exportToCsv(eq(schedule), eq("Auto Loan 20%"), any(OutputStream.class));
    }

    @Test
    void compareWithoutLoanIdsDoesNotFailAndLoadsDefaults() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setName("Loan 20% Down");
        loan1.setPrincipal(28000.0);
        loan1.setAnnualInterestRate(5.0);
        loan1.setTermMonths(60);

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setName("Loan 30% Down");
        loan2.setPrincipal(24500.0);
        loan2.setAnnualInterestRate(5.0);
        loan2.setTermMonths(60);

        when(loanService.findAll()).thenReturn(List.of(loan1, loan2));
        when(amortizationService.generateCompareSummary(any(Loan.class), eq(true)))
                .thenReturn(new AmortizationService.LoanCompareSummary(
                        1L, "Test", 35000.0, 7000.0, 20.0, 28000.0, 5.0, 60, 528.0, 3700.0, 60, 31700.0, List.of()
                ));

        mockMvc.perform(get("/loans/compare"))
                .andExpect(status().isOk())
                .andExpect(view().name("loans/compare"))
                .andExpect(model().attributeExists("allLoans"))
                .andExpect(model().attributeExists("summaries"));
    }

    @Test
    void compareWithSelectedLoanIdsLoadsSelectedSummaries() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(10L);
        loan1.setName("Car 30% Down");

        when(loanService.findAll()).thenReturn(List.of(loan1));
        when(loanService.findByIdOrThrow(10L)).thenReturn(loan1);
        when(amortizationService.generateCompareSummary(loan1, true))
                .thenReturn(new AmortizationService.LoanCompareSummary(
                        10L, "Car 30% Down", 30000.0, 9000.0, 30.0, 21000.0, 5.0, 48, 483.0, 2200.0, 48, 23200.0, List.of()
                ));

        mockMvc.perform(get("/loans/compare").param("loanIds", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("loans/compare"))
                .andExpect(model().attributeExists("summaries"));
    }

    @Test
    void exportComparePdfReturnsPdfAndCallsExportService() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setName("Scenario 20%");

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setName("Scenario 30%");

        when(loanService.findAll()).thenReturn(List.of(loan1, loan2));
        when(amortizationService.generateCompareSummary(any(Loan.class), eq(true)))
            .thenReturn(new AmortizationService.LoanCompareSummary(
                1L, "Scenario 20%", 35000.0, 7000.0, 20.0, 28000.0, 5.0, 60, 528.0, 3700.0, 60, 31700.0, List.of()
            ));

        mockMvc.perform(get("/loans/compare/export/pdf"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan_comparison_report.pdf\""));

        verify(exportService).exportComparisonPdf(any(), any(OutputStream.class));
    }
}
