package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.AmortizationService.LoanCompareSummary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ExportService {

    // Palette
    private static final Color COLOR_PRIMARY = new Color(30, 41, 59);        // Slate 800
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);     // Slate 600
    private static final Color COLOR_ACCENT = new Color(37, 99, 235);        // Blue 600
    private static final Color COLOR_ACCENT_GREEN = new Color(22, 163, 74);  // Green 600
    private static final Color COLOR_ACCENT_RED = new Color(220, 38, 38);    // Red 600
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);    // Slate 50
    private static final Color COLOR_BG_GREEN = new Color(240, 253, 244);    // Emerald 50
    private static final Color COLOR_BORDER_GREEN = new Color(134, 239, 172);// Emerald 300
    private static final Color COLOR_BORDER = new Color(203, 213, 225);      // Slate 300
    private static final Color COLOR_BORDER_LIGHT = new Color(226, 232, 240);// Slate 200
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);      // Slate 900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);  // Slate 500
    private static final Color COLOR_WHITE = Color.WHITE;

    // Dimensions for Single Loan Schedule (A4 Portrait: 595 x 842 pt)
    private static final float PORTRAIT_WIDTH = 595f;
    private static final float PORTRAIT_HEIGHT = 842f;
    private static final float PORTRAIT_MARGIN_X = 36f;
    private static final float PORTRAIT_CONTENT_WIDTH = 523f; // 595 - 72

    // Schedule Table Column widths (must sum to 523f)
    private static final float[] SCHEDULE_COL_WIDTHS = {35f, 75f, 75f, 75f, 85f, 85f, 93f};
    private static final String[] SCHEDULE_HEADERS = {
        "Month", "Payment ($)", "Principal ($)", "Interest ($)", "Total Princ. ($)", "Total Int. ($)", "Balance ($)"
    };

    // Dimensions for Compare Loans (A4 Landscape: 842 x 595 pt)
    private static final float LANDSCAPE_WIDTH = 842f;
    private static final float LANDSCAPE_HEIGHT = 595f;
    private static final float LANDSCAPE_MARGIN_X = 36f;
    private static final float LANDSCAPE_CONTENT_WIDTH = 770f; // 842 - 72

    public void exportToCsv(List<AmortizationRow> schedule, String loanName, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
        writer.println("Month,Payment,Principal,Interest,Total Principal,Total Interest,Remaining Balance");

        for (AmortizationRow row : schedule) {
            writer.printf(Locale.US, "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                row.month(),
                row.payment(),
                row.principalPaid(),
                row.interestPaid(),
                row.totalPrincipalPaid(),
                row.totalInterestPaid(),
                row.remainingBalance()
            );
        }

        writer.close();
    }

    /**
     * Backward-compatible overload for exporting loan schedule to PDF.
     */
    public void exportToPdf(List<AmortizationRow> schedule, String loanName, OutputStream out) throws IOException {
        Loan loan = new Loan();
        loan.setName(loanName);
        if (schedule != null && !schedule.isEmpty()) {
            AmortizationRow first = schedule.get(0);
            AmortizationRow last = schedule.get(schedule.size() - 1);
            loan.setPrincipal(first.remainingBalance() + first.principalPaid());
            loan.setTermMonths(last.month());
            loan.setAnnualInterestRate(0.0);
        }
        exportLoanToPdf(loan, schedule, null, false, out);
    }

    /**
     * Rich single-loan PDF export with summary cards, optional extra-payments savings banner,
     * beautifully styled multi-page table, and footers.
     */
    public void exportLoanToPdf(Loan loan, List<AmortizationRow> schedule,
                                PayoffComparison payoffComparison, boolean includeExtra,
                                OutputStream out) throws IOException {
        PDDocument document = new PDDocument();
        PDRectangle pageSize = new PDRectangle(PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        float[] columnStarts = calculateStarts(PORTRAIT_MARGIN_X, SCHEDULE_COL_WIDTHS);

        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(document, page);

        String loanName = loan.getName() != null ? loan.getName() : "Loan Amortization";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));

        try {
            // Top Accent Bar
            drawRect(cs, COLOR_PRIMARY, null, 0, PORTRAIT_MARGIN_X, PORTRAIT_HEIGHT - 32f, PORTRAIT_CONTENT_WIDTH, 4f);

            // Header Section
            float headerY = PORTRAIT_HEIGHT - 54f;
            drawText(cs, "LOAN AMORTIZATION SCHEDULE", PDType1Font.HELVETICA_BOLD, 16f, COLOR_PRIMARY, PORTRAIT_MARGIN_X, headerY);
            drawText(cs, "Scenario: " + loanName, PDType1Font.HELVETICA_BOLD, 10.5f, COLOR_SECONDARY, PORTRAIT_MARGIN_X, headerY - 16f);

            // Right Header Metadata
            drawRightAlignedText(cs, "Generated: " + dateStr, PDType1Font.HELVETICA, 8.5f, COLOR_TEXT_MUTED, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, headerY);
            drawRightAlignedText(cs, "Amortization Financial Statement", PDType1Font.HELVETICA, 8f, COLOR_TEXT_MUTED, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, headerY - 14f);

            // Divider Line
            float divY = headerY - 26f;
            drawLine(cs, COLOR_BORDER_LIGHT, 0.75f, PORTRAIT_MARGIN_X, divY, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, divY);

            // Summary Metrics Box (2 rows x 4 cols)
            float summaryBoxTop = divY - 8f;
            float summaryBoxHeight = 58f;
            drawRect(cs, COLOR_BG_LIGHT, COLOR_BORDER, 0.75f, PORTRAIT_MARGIN_X, summaryBoxTop - summaryBoxHeight, PORTRAIT_CONTENT_WIDTH, summaryBoxHeight);

            double principal = loan.getPrincipal() != null ? loan.getPrincipal() : 0.0;
            double rate = loan.getAnnualInterestRate() != null ? loan.getAnnualInterestRate() : 0.0;
            int term = loan.getTermMonths() != null ? loan.getTermMonths() : (schedule != null ? schedule.size() : 0);
            double monthlyPayment = 0.0;
            double totalInterest = 0.0;
            if (schedule != null && !schedule.isEmpty()) {
                monthlyPayment = schedule.get(0).payment();
                totalInterest = schedule.get(schedule.size() - 1).totalInterestPaid();
            }
            double totalCost = principal + totalInterest;

            float colW = PORTRAIT_CONTENT_WIDTH / 4f;

            // Row 1 metrics
            float r1LabelY = summaryBoxTop - 14f;
            float r1ValY = summaryBoxTop - 25f;

            // Col 1: Vehicle / Total Price
            drawText(cs, "CAR / TOTAL COST", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 8f, r1LabelY);
            String totalPriceStr = (loan.getTotalPrice() != null && loan.getTotalPrice() > 0) ? formatCurrency(loan.getTotalPrice()) : "-";
            drawText(cs, totalPriceStr, PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_TEXT_DARK, PORTRAIT_MARGIN_X + 8f, r1ValY);

            // Col 2: Down Payment
            drawText(cs, "DOWN PAYMENT", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + colW + 8f, r1LabelY);
            String dpStr = "-";
            if (loan.getDownPayment() != null && loan.getDownPayment() > 0) {
                dpStr = formatCurrency(loan.getDownPayment());
                if (loan.getDownPaymentPercent() != null) {
                    dpStr += String.format(Locale.US, " (%.0f%%)", loan.getDownPaymentPercent());
                }
            }
            drawText(cs, dpStr, PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_ACCENT_GREEN, PORTRAIT_MARGIN_X + colW + 8f, r1ValY);

            // Col 3: Financed Principal
            drawText(cs, "FINANCED PRINCIPAL", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 2 * colW + 8f, r1LabelY);
            drawText(cs, formatCurrency(principal), PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_TEXT_DARK, PORTRAIT_MARGIN_X + 2 * colW + 8f, r1ValY);

            // Col 4: Interest Rate
            drawText(cs, "INTEREST RATE", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 3 * colW + 8f, r1LabelY);
            drawText(cs, String.format(Locale.US, "%.2f%% APR", rate), PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_TEXT_DARK, PORTRAIT_MARGIN_X + 3 * colW + 8f, r1ValY);

            // Row 2 metrics
            float r2LabelY = summaryBoxTop - 38f;
            float r2ValY = summaryBoxTop - 49f;

            // Col 1: Loan Term
            drawText(cs, "LOAN TERM", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 8f, r2LabelY);
            drawText(cs, term + " Months", PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_TEXT_DARK, PORTRAIT_MARGIN_X + 8f, r2ValY);

            // Col 2: Monthly Payment
            drawText(cs, "MONTHLY PAYMENT", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + colW + 8f, r2LabelY);
            drawText(cs, formatCurrency(monthlyPayment), PDType1Font.HELVETICA_BOLD, 10f, COLOR_ACCENT, PORTRAIT_MARGIN_X + colW + 8f, r2ValY);

            // Col 3: Total Interest
            drawText(cs, "TOTAL INTEREST", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 2 * colW + 8f, r2LabelY);
            drawText(cs, formatCurrency(totalInterest), PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_ACCENT_RED, PORTRAIT_MARGIN_X + 2 * colW + 8f, r2ValY);

            // Col 4: Total Loan Cost
            drawText(cs, "TOTAL LOAN COST", PDType1Font.HELVETICA_BOLD, 7f, COLOR_TEXT_MUTED, PORTRAIT_MARGIN_X + 3 * colW + 8f, r2LabelY);
            drawText(cs, formatCurrency(totalCost), PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_TEXT_DARK, PORTRAIT_MARGIN_X + 3 * colW + 8f, r2ValY);

            float currentY = summaryBoxTop - summaryBoxHeight - 10f;

            // Extra Payments Acceleration Impact Banner (if applicable)
            if (includeExtra && payoffComparison != null && payoffComparison.hasActiveExtraPayments() && payoffComparison.monthsSaved() > 0) {
                float bannerHeight = 32f;
                drawRect(cs, COLOR_BG_GREEN, COLOR_BORDER_GREEN, 0.75f, PORTRAIT_MARGIN_X, currentY - bannerHeight, PORTRAIT_CONTENT_WIDTH, bannerHeight);

                drawText(cs, "EXTRA PAYMENTS ACCELERATION IMPACT", PDType1Font.HELVETICA_BOLD, 7.5f, new Color(21, 128, 61), PORTRAIT_MARGIN_X + 8f, currentY - 11f);
                String impactDetails = String.format(Locale.US,
                    "Payoff in %d months (Saved %d months)  |  Interest Saved: %s  |  New Total Cost: %s",
                    payoffComparison.acceleratedMonths(),
                    payoffComparison.monthsSaved(),
                    formatCurrency(payoffComparison.interestSaved()),
                    formatCurrency(payoffComparison.acceleratedTotalCost())
                );
                drawText(cs, impactDetails, PDType1Font.HELVETICA, 8.5f, new Color(20, 83, 45), PORTRAIT_MARGIN_X + 8f, currentY - 24f);

                currentY -= (bannerHeight + 10f);
            }

            // Draw Schedule Table Header
            currentY = drawScheduleTableHeader(cs, columnStarts, currentY);

            // Draw Schedule Rows
            if (schedule != null) {
                int rowIndex = 0;
                for (AmortizationRow row : schedule) {
                    if (currentY < 48f) {
                        // Overflow to next page
                        cs.close();
                        page = new PDPage(pageSize);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);

                        // Continuation header
                        drawText(cs, "Amortization Schedule - " + loanName + " (Continued)", PDType1Font.HELVETICA_BOLD, 9f, COLOR_SECONDARY, PORTRAIT_MARGIN_X, PORTRAIT_HEIGHT - 38f);
                        drawRightAlignedText(cs, "Page " + document.getNumberOfPages(), PDType1Font.HELVETICA, 8f, COLOR_TEXT_MUTED, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, PORTRAIT_HEIGHT - 38f);
                        drawLine(cs, COLOR_BORDER_LIGHT, 0.5f, PORTRAIT_MARGIN_X, PORTRAIT_HEIGHT - 44f, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, PORTRAIT_HEIGHT - 44f);

                        currentY = drawScheduleTableHeader(cs, columnStarts, PORTRAIT_HEIGHT - 52f);
                    }

                    currentY = drawScheduleRow(cs, row, columnStarts, currentY, rowIndex % 2 == 1);
                    rowIndex++;
                }
            }
        } finally {
            cs.close();
        }

        // Add 2-pass footers across all pages
        addPageFooters(document, "Loan Amortization Statement - " + loanName, PORTRAIT_WIDTH, PORTRAIT_MARGIN_X);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();

        out.write(baos.toByteArray());
        out.close();
    }

    /**
     * Exports a side-by-side scenario comparison to an A4 Landscape PDF with executive highlights,
     * side-by-side matrix, best-value indicators, and summary notes.
     */
    public void exportComparisonPdf(List<LoanCompareSummary> summaries, OutputStream out) throws IOException {
        PDDocument document = new PDDocument();
        PDRectangle pageSize = new PDRectangle(LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);

        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(document, page);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
        int scenarioCount = summaries != null ? summaries.size() : 0;

        try {
            // Top Accent Bar
            drawRect(cs, COLOR_PRIMARY, null, 0, LANDSCAPE_MARGIN_X, LANDSCAPE_HEIGHT - 28f, LANDSCAPE_CONTENT_WIDTH, 4f);

            // Header Section
            float headerY = LANDSCAPE_HEIGHT - 48f;
            drawText(cs, "LOAN SCENARIO COMPARISON REPORT", PDType1Font.HELVETICA_BOLD, 16f, COLOR_PRIMARY, LANDSCAPE_MARGIN_X, headerY);
            drawText(cs, "Side-by-side analysis of monthly payments, financing terms, and total interest costs",
                PDType1Font.HELVETICA, 9.5f, COLOR_SECONDARY, LANDSCAPE_MARGIN_X, headerY - 14f);

            // Right Header Metadata
            drawRightAlignedText(cs, "Generated: " + dateStr, PDType1Font.HELVETICA, 8.5f, COLOR_TEXT_MUTED, LANDSCAPE_WIDTH - LANDSCAPE_MARGIN_X, headerY);
            drawRightAlignedText(cs, "Scenarios Analyzed: " + scenarioCount, PDType1Font.HELVETICA_BOLD, 8.5f, COLOR_PRIMARY, LANDSCAPE_WIDTH - LANDSCAPE_MARGIN_X, headerY - 14f);

            // Divider Line
            float divY = headerY - 22f;
            drawLine(cs, COLOR_BORDER_LIGHT, 0.75f, LANDSCAPE_MARGIN_X, divY, LANDSCAPE_WIDTH - LANDSCAPE_MARGIN_X, divY);

            float currentY = divY - 8f;

            if (summaries != null && summaries.size() >= 2) {
                // Determine highlights
                LoanCompareSummary minMonthly = summaries.stream().min(Comparator.comparingDouble(LoanCompareSummary::monthlyPayment)).orElse(summaries.get(0));
                LoanCompareSummary maxMonthly = summaries.stream().max(Comparator.comparingDouble(LoanCompareSummary::monthlyPayment)).orElse(summaries.get(0));

                LoanCompareSummary minInterest = summaries.stream().min(Comparator.comparingDouble(LoanCompareSummary::totalInterest)).orElse(summaries.get(0));
                LoanCompareSummary maxInterest = summaries.stream().max(Comparator.comparingDouble(LoanCompareSummary::totalInterest)).orElse(summaries.get(0));

                LoanCompareSummary minMonths = summaries.stream().min(Comparator.comparingInt(LoanCompareSummary::monthsToPayoff)).orElse(summaries.get(0));
                LoanCompareSummary maxMonths = summaries.stream().max(Comparator.comparingInt(LoanCompareSummary::monthsToPayoff)).orElse(summaries.get(0));

                // 3 Highlights Cards
                float cardSpacing = 10f;
                float cardW = (LANDSCAPE_CONTENT_WIDTH - 2 * cardSpacing) / 3f;
                float cardH = 42f;
                float cardY = currentY - cardH;

                // Card 1: Lowest Monthly Payment
                float c1X = LANDSCAPE_MARGIN_X;
                drawRect(cs, COLOR_BG_GREEN, COLOR_BORDER_GREEN, 0.75f, c1X, cardY, cardW, cardH);
                drawText(cs, "LOWEST MONTHLY PAYMENT", PDType1Font.HELVETICA_BOLD, 7.5f, new Color(21, 128, 61), c1X + 8f, cardY + 29f);
                drawText(cs, minMonthly.name() + ": " + formatCurrency(minMonthly.monthlyPayment()) + "/mo",
                    PDType1Font.HELVETICA_BOLD, 9.5f, new Color(22, 101, 52), c1X + 8f, cardY + 16f);
                double monthlySavings = maxMonthly.monthlyPayment() - minMonthly.monthlyPayment();
                String monthlySub = monthlySavings > 0.01
                    ? String.format(Locale.US, "Saves %s/mo vs highest", formatCurrency(monthlySavings))
                    : "Identical across scenarios";
                drawText(cs, monthlySub, PDType1Font.HELVETICA, 7.5f, new Color(22, 101, 52), c1X + 8f, cardY + 5f);

                // Card 2: Lowest Total Interest
                float c2X = c1X + cardW + cardSpacing;
                drawRect(cs, new Color(254, 242, 242), new Color(252, 165, 165), 0.75f, c2X, cardY, cardW, cardH);
                drawText(cs, "LOWEST TOTAL INTEREST", PDType1Font.HELVETICA_BOLD, 7.5f, new Color(153, 27, 27), c2X + 8f, cardY + 29f);
                drawText(cs, minInterest.name() + ": " + formatCurrency(minInterest.totalInterest()),
                    PDType1Font.HELVETICA_BOLD, 9.5f, new Color(185, 28, 28), c2X + 8f, cardY + 16f);
                double interestSavings = maxInterest.totalInterest() - minInterest.totalInterest();
                String interestSub = interestSavings > 0.01
                    ? String.format(Locale.US, "Saves %s in interest vs highest", formatCurrency(interestSavings))
                    : "Identical across scenarios";
                drawText(cs, interestSub, PDType1Font.HELVETICA, 7.5f, new Color(153, 27, 27), c2X + 8f, cardY + 5f);

                // Card 3: Fastest Payoff
                float c3X = c2X + cardW + cardSpacing;
                drawRect(cs, new Color(239, 246, 255), new Color(147, 197, 253), 0.75f, c3X, cardY, cardW, cardH);
                drawText(cs, "FASTEST PAYOFF DURATION", PDType1Font.HELVETICA_BOLD, 7.5f, new Color(30, 64, 175), c3X + 8f, cardY + 29f);
                drawText(cs, minMonths.name() + ": " + minMonths.monthsToPayoff() + " months",
                    PDType1Font.HELVETICA_BOLD, 9.5f, new Color(29, 78, 216), c3X + 8f, cardY + 16f);
                int timeDiff = maxMonths.monthsToPayoff() - minMonths.monthsToPayoff();
                String timeSub = timeDiff > 0
                    ? String.format(Locale.US, "Pays off %d months sooner", timeDiff)
                    : "Equal duration across scenarios";
                drawText(cs, timeSub, PDType1Font.HELVETICA, 7.5f, new Color(30, 64, 175), c3X + 8f, cardY + 5f);

                currentY = cardY - 12f;

                // Side-by-Side Matrix Table
                float rowHeaderWidth = 190f;
                float scenarioColWidth = (LANDSCAPE_CONTENT_WIDTH - rowHeaderWidth) / scenarioCount;

                // Matrix Header
                float tableHeaderHeight = 30f;
                drawRect(cs, COLOR_PRIMARY, null, 0, LANDSCAPE_MARGIN_X, currentY - tableHeaderHeight, LANDSCAPE_CONTENT_WIDTH, tableHeaderHeight);

                drawText(cs, "Scenario / Metric", PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_WHITE,
                    LANDSCAPE_MARGIN_X + 10f, currentY - 18f);

                for (int i = 0; i < scenarioCount; i++) {
                    LoanCompareSummary s = summaries.get(i);
                    float colX = LANDSCAPE_MARGIN_X + rowHeaderWidth + i * scenarioColWidth;

                    drawCenteredText(cs, s.name(), PDType1Font.HELVETICA_BOLD, 9.5f, COLOR_WHITE,
                        colX + scenarioColWidth / 2f, currentY - 13f);

                    String subtitle = "";
                    if (s.downPaymentPercent() != null && s.downPaymentPercent() > 0) {
                        subtitle = String.format(Locale.US, "(%.0f%% Down)", s.downPaymentPercent());
                    } else if (s.downPayment() != null && s.downPayment() > 0) {
                        subtitle = String.format(Locale.US, "(%s Down)", formatCurrency(s.downPayment()));
                    }
                    if (!subtitle.isEmpty()) {
                        drawCenteredText(cs, subtitle, PDType1Font.HELVETICA, 7.5f, new Color(203, 213, 225),
                            colX + scenarioColWidth / 2f, currentY - 24f);
                    }
                }

                currentY -= tableHeaderHeight;

                // Rows Definition
                float rowH = 20.5f;

                // Row 1: Car / Purchase Cost
                currentY = drawMatrixRow(cs, "Car / Total Purchase Price", summaries, s ->
                    (s.totalPrice() != null && s.totalPrice() > 0) ? formatCurrency(s.totalPrice()) : "-",
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, false, null, null);

                // Row 2: Down Payment
                currentY = drawMatrixRow(cs, "First Payment / Down Payment", summaries, s -> {
                    if (s.downPayment() != null && s.downPayment() > 0) {
                        String pct = s.downPaymentPercent() != null ? String.format(Locale.US, " (%.1f%%)", s.downPaymentPercent()) : "";
                        return formatCurrency(s.downPayment()) + pct;
                    }
                    return "-";
                }, LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, true, null, COLOR_ACCENT_GREEN);

                // Row 3: Financed Principal
                currentY = drawMatrixRow(cs, "Financed Principal", summaries, s ->
                    formatCurrency(s.principal()),
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, false, null, null);

                // Row 4: Annual Interest Rate
                currentY = drawMatrixRow(cs, "Annual Interest Rate (APR)", summaries, s ->
                    String.format(Locale.US, "%.2f%%", s.annualInterestRate()),
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, true, null, null);

                // Row 5: Term Duration
                currentY = drawMatrixRow(cs, "Term Duration", summaries, s ->
                    s.termMonths() + " months",
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, false, null, null);

                // Row 6: Monthly Payment (Highlighted green row!)
                currentY = drawMatrixRow(cs, "Monthly Payment", summaries, s ->
                    formatCurrency(s.monthlyPayment()) + (s.id().equals(minMonthly.id()) && monthlySavings > 0.01 ? " [Lowest]" : ""),
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, true, COLOR_BG_GREEN, new Color(21, 128, 61));

                // Row 7: Total Interest Paid
                currentY = drawMatrixRow(cs, "Total Interest Paid", summaries, s ->
                    formatCurrency(s.totalInterest()) + (s.id().equals(minInterest.id()) && interestSavings > 0.01 ? " [Lowest]" : ""),
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, false, null, COLOR_ACCENT_RED);

                // Row 8: Payoff Time
                currentY = drawMatrixRow(cs, "Payoff Time", summaries, s ->
                    s.monthsToPayoff() + " months",
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, true, null, null);

                // Row 9: Total Loan Cost (Financed + Int.)
                currentY = drawMatrixRow(cs, "Total Loan Cost (Financed + Int.)", summaries, s ->
                    formatCurrency(s.totalCost()),
                    LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, false, null, COLOR_TEXT_DARK);

                // Row 10: Total Out of Pocket (Down Pmt + Cost)
                currentY = drawMatrixRow(cs, "Total Purchase Cost (With Down Pmt)", summaries, s -> {
                    double dp = s.downPayment() != null ? s.downPayment() : 0.0;
                    return formatCurrency(s.totalCost() + dp);
                }, LANDSCAPE_MARGIN_X, rowHeaderWidth, scenarioColWidth, currentY, rowH, true, null, COLOR_TEXT_DARK);

                // Executive Insights & Takeaways Box
                currentY -= 12f;
                float notesH = 68f;
                drawRect(cs, COLOR_BG_LIGHT, COLOR_BORDER, 0.75f, LANDSCAPE_MARGIN_X, currentY - notesH, LANDSCAPE_CONTENT_WIDTH, notesH);

                float noteTextX = LANDSCAPE_MARGIN_X + 10f;
                drawText(cs, "FINANCIAL COMPARISON SUMMARY & TAKEAWAYS", PDType1Font.HELVETICA_BOLD, 8f, COLOR_PRIMARY, noteTextX, currentY - 13f);

                String note1 = String.format(Locale.US,
                    "* Down Payment Impact: Higher down payment reduces both financed principal and interest accrued over the life of the loan.");
                drawText(cs, note1, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, noteTextX, currentY - 26f);

                String note2 = String.format(Locale.US,
                    "* Interest Cost: Scenario '%s' yields maximum interest savings of %s compared to '%s'.",
                    minInterest.name(), formatCurrency(interestSavings), maxInterest.name());
                drawText(cs, note2, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, noteTextX, currentY - 38f);

                String note3 = String.format(Locale.US,
                    "* Monthly Cash Flow: Scenario '%s' offers the lowest monthly commitment at %s/mo (%s less per month).",
                    minMonthly.name(), formatCurrency(minMonthly.monthlyPayment()), formatCurrency(monthlySavings));
                drawText(cs, note3, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, noteTextX, currentY - 50f);

                String note4 = "* Extra payments (if configured) are factored into total interest and payoff duration.";
                drawText(cs, note4, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, noteTextX, currentY - 62f);
            } else {
                drawText(cs, "At least two loan scenarios are required to generate comparison metrics.",
                    PDType1Font.HELVETICA, 11f, COLOR_TEXT_MUTED, LANDSCAPE_MARGIN_X, currentY - 30f);
            }
        } finally {
            cs.close();
        }

        // Add 2-pass footers across all landscape pages
        addPageFooters(document, "Loan Scenario Comparison Report", LANDSCAPE_WIDTH, LANDSCAPE_MARGIN_X);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();

        out.write(baos.toByteArray());
        out.close();
    }

    private float drawScheduleTableHeader(PDPageContentStream cs, float[] colStarts, float y) throws IOException {
        float headerHeight = 17f;
        drawRect(cs, COLOR_PRIMARY, null, 0, PORTRAIT_MARGIN_X, y - headerHeight, PORTRAIT_CONTENT_WIDTH, headerHeight);

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 8f);
        cs.setNonStrokingColor(COLOR_WHITE);

        for (int i = 0; i < SCHEDULE_HEADERS.length; i++) {
            float textW = PDType1Font.HELVETICA_BOLD.getStringWidth(SCHEDULE_HEADERS[i]) / 1000f * 8f;
            float x;
            if (i == 0) {
                x = colStarts[0] + 4f;
            } else {
                x = colStarts[i] + SCHEDULE_COL_WIDTHS[i] - 4f - textW;
            }
            cs.setTextMatrix(new Matrix(1, 0, 0, 1, x, y - 11.5f));
            cs.showText(SCHEDULE_HEADERS[i]);
        }
        cs.endText();

        return y - headerHeight;
    }

    private float drawScheduleRow(PDPageContentStream cs, AmortizationRow row, float[] colStarts, float y, boolean zebra) throws IOException {
        float rowHeight = 13.5f;

        if (zebra) {
            drawRect(cs, COLOR_BG_LIGHT, null, 0, PORTRAIT_MARGIN_X, y - rowHeight, PORTRAIT_CONTENT_WIDTH, rowHeight);
        }
        drawLine(cs, COLOR_BORDER_LIGHT, 0.35f, PORTRAIT_MARGIN_X, y - rowHeight, PORTRAIT_WIDTH - PORTRAIT_MARGIN_X, y - rowHeight);

        String[] cells = {
            String.valueOf(row.month()),
            formatNumber(row.payment()),
            formatNumber(row.principalPaid()),
            formatNumber(row.interestPaid()),
            formatNumber(row.totalPrincipalPaid()),
            formatNumber(row.totalInterestPaid()),
            formatNumber(row.remainingBalance())
        };

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.setNonStrokingColor(COLOR_TEXT_DARK);

        for (int i = 0; i < cells.length; i++) {
            float textW = PDType1Font.HELVETICA.getStringWidth(cells[i]) / 1000f * 7.5f;
            float x;
            if (i == 0) {
                x = colStarts[0] + 4f;
            } else {
                x = colStarts[i] + SCHEDULE_COL_WIDTHS[i] - 4f - textW;
            }
            cs.setTextMatrix(new Matrix(1, 0, 0, 1, x, y - 9.5f));
            cs.showText(cells[i]);
        }
        cs.endText();

        return y - rowHeight;
    }

    @FunctionalInterface
    private interface SummaryValueExtractor {
        String extract(LoanCompareSummary summary);
    }

    private float drawMatrixRow(PDPageContentStream cs, String label, List<LoanCompareSummary> summaries,
                                SummaryValueExtractor extractor, float startX, float rowHeaderW,
                                float scenarioColW, float y, float rowH, boolean zebra,
                                Color overrideBg, Color overrideTextColor) throws IOException {
        Color bg = overrideBg != null ? overrideBg : (zebra ? COLOR_BG_LIGHT : COLOR_WHITE);
        drawRect(cs, bg, null, 0, startX, y - rowH, LANDSCAPE_CONTENT_WIDTH, rowH);
        drawLine(cs, COLOR_BORDER_LIGHT, 0.4f, startX, y - rowH, startX + LANDSCAPE_CONTENT_WIDTH, y - rowH);

        // Header column
        drawText(cs, label, PDType1Font.HELVETICA_BOLD, 8.5f,
            overrideBg != null ? new Color(22, 101, 52) : COLOR_TEXT_DARK,
            startX + 8f, y - 13.5f);

        // Scenario columns
        for (int i = 0; i < summaries.size(); i++) {
            LoanCompareSummary s = summaries.get(i);
            float colX = startX + rowHeaderW + i * scenarioColW;
            String val = extractor.extract(s);
            Color textCol = overrideTextColor != null ? overrideTextColor : COLOR_TEXT_DARK;
            PDType1Font font = (overrideBg != null || overrideTextColor != null) ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
            float fontSize = overrideBg != null ? 9.5f : 8.5f;

            drawCenteredText(cs, val, font, fontSize, textCol, colX + scenarioColW / 2f, y - 13.5f);
        }

        return y - rowH;
    }

    private void addPageFooters(PDDocument document, String reportName, float pageWidth, float marginX) throws IOException {
        int totalPages = document.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                float y = 20f;
                drawLine(cs, COLOR_BORDER_LIGHT, 0.5f, marginX, y + 8f, pageWidth - marginX, y + 8f);

                drawText(cs, "Amortization Calculator  |  " + reportName, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, marginX, y);

                String pageStr = "Page " + (i + 1) + " of " + totalPages;
                drawRightAlignedText(cs, pageStr, PDType1Font.HELVETICA, 7.5f, COLOR_TEXT_MUTED, pageWidth - marginX, y);
            }
        }
    }

    private float[] calculateStarts(float startX, float[] widths) {
        float[] starts = new float[widths.length];
        float x = startX;
        for (int i = 0; i < widths.length; i++) {
            starts[i] = x;
            x += widths[i];
        }
        return starts;
    }

    private void drawRect(PDPageContentStream cs, Color fill, Color stroke, float strokeWidth, float x, float y, float width, float height) throws IOException {
        if (fill != null) {
            cs.setNonStrokingColor(fill);
            cs.addRect(x, y, width, height);
            cs.fill();
        }
        if (stroke != null) {
            cs.setStrokingColor(stroke);
            cs.setLineWidth(strokeWidth > 0 ? strokeWidth : 0.5f);
            cs.addRect(x, y, width, height);
            cs.stroke();
        }
    }

    private void drawLine(PDPageContentStream cs, Color stroke, float strokeWidth, float x1, float y1, float x2, float y2) throws IOException {
        cs.setStrokingColor(stroke);
        cs.setLineWidth(strokeWidth > 0 ? strokeWidth : 0.5f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void drawText(PDPageContentStream cs, String text, PDType1Font font, float fontSize, Color color, float x, float y) throws IOException {
        String cleanText = sanitize(text);
        if (cleanText.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.setTextMatrix(new Matrix(1, 0, 0, 1, x, y));
        cs.showText(cleanText);
        cs.endText();
    }

    private void drawRightAlignedText(PDPageContentStream cs, String text, PDType1Font font, float fontSize, Color color, float rightX, float y) throws IOException {
        String cleanText = sanitize(text);
        if (cleanText.isEmpty()) return;
        float width = font.getStringWidth(cleanText) / 1000f * fontSize;
        drawText(cs, cleanText, font, fontSize, color, rightX - width, y);
    }

    private void drawCenteredText(PDPageContentStream cs, String text, PDType1Font font, float fontSize, Color color, float centerX, float y) throws IOException {
        String cleanText = sanitize(text);
        if (cleanText.isEmpty()) return;
        float width = font.getStringWidth(cleanText) / 1000f * fontSize;
        drawText(cs, cleanText, font, fontSize, color, centerX - width / 2f, y);
    }

    private String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c == '\u2014' || c == '\u2013') {
                sb.append('-');
            } else if (c == '\u2022' || c == '\u00B7') {
                sb.append('*');
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    private String formatCurrency(double val) {
        return String.format(Locale.US, "$%,.2f", val);
    }

    private String formatNumber(double val) {
        return String.format(Locale.US, "%,.2f", val);
    }
}
