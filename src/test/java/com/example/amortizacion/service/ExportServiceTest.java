package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.AmortizationService.LoanCompareSummary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    @Test
    void exportToCsvGeneratesValidCsv() throws IOException {
        List<AmortizationRow> schedule = List.of(
            new AmortizationRow(1, 500.0, 400.0, 100.0, 400.0, 100.0, 9600.0),
            new AmortizationRow(2, 500.0, 405.0, 95.0, 805.0, 195.0, 9195.0)
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportToCsv(schedule, "Test Loan", out);

        String csv = out.toString();
        assertThat(csv).contains("Month,Payment,Principal,Interest,Total Principal,Total Interest,Remaining Balance");
        assertThat(csv).contains("1,500.00,400.00,100.00,400.00,100.00,9600.00");
        assertThat(csv).contains("2,500.00,405.00,95.00,805.00,195.00,9195.00");
    }

    @Test
    void exportLoanToPdfGeneratesValidPdfWithSummaryAndSchedule() throws IOException {
        Loan loan = new Loan();
        loan.setName("Honda CR-V 20% Down");
        loan.setTotalPrice(35000.0);
        loan.setDownPayment(7000.0);
        loan.setDownPaymentPercent(20.0);
        loan.setPrincipal(28000.0);
        loan.setAnnualInterestRate(5.5);
        loan.setTermMonths(60);

        List<AmortizationRow> schedule = new ArrayList<>();
        double balance = 28000.0;
        for (int m = 1; m <= 60; m++) {
            balance -= 400.0;
            schedule.add(new AmortizationRow(m, 534.68, 400.0, 134.68, m * 400.0, m * 134.68, Math.max(0, balance)));
        }

        PayoffComparison comparison = new PayoffComparison(
            60, 48, 12, 4200.0, 3100.0, 1100.0, 32200.0, 31100.0, 2400.0, true
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportLoanToPdf(loan, schedule, comparison, true, out);

        byte[] pdfBytes = out.toByteArray();
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("LOAN AMORTIZATION SCHEDULE");
            assertThat(text).contains("Honda CR-V 20% Down");
            assertThat(text).contains("FINANCED PRINCIPAL");
            assertThat(text).contains("$28,000.00");
            assertThat(text).contains("EXTRA PAYMENTS ACCELERATION IMPACT");
            assertThat(text).contains("Page 1 of");
        }
    }

    @Test
    void exportComparisonPdfGeneratesValidLandscapePdfWithMatrix() throws IOException {
        LoanCompareSummary s1 = new LoanCompareSummary(
            1L, "20% Down Payment", 35000.0, 7000.0, 20.0, 28000.0, 5.5, 60, 534.68, 4080.80, 60, 32080.80, List.of()
        );
        LoanCompareSummary s2 = new LoanCompareSummary(
            2L, "30% Down Payment", 35000.0, 10500.0, 30.0, 24500.0, 5.5, 60, 467.85, 3571.00, 60, 28071.00, List.of()
        );
        LoanCompareSummary s3 = new LoanCompareSummary(
            3L, "40% Down Payment", 35000.0, 14000.0, 40.0, 21000.0, 5.5, 48, 488.10, 2428.80, 48, 23428.80, List.of()
        );

        List<LoanCompareSummary> summaries = List.of(s1, s2, s3);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportComparisonPdf(summaries, out);

        byte[] pdfBytes = out.toByteArray();
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("LOAN SCENARIO COMPARISON REPORT");
            assertThat(text).contains("20% Down Payment");
            assertThat(text).contains("30% Down Payment");
            assertThat(text).contains("40% Down Payment");
            assertThat(text).contains("LOWEST MONTHLY PAYMENT");
            assertThat(text).contains("LOWEST TOTAL INTEREST");
            assertThat(text).contains("Monthly Payment");
            assertThat(text).contains("Total Interest Paid");
            assertThat(text).contains("FINANCIAL COMPARISON SUMMARY");
            assertThat(text).contains("Page 1 of 1");
        }
    }
}

