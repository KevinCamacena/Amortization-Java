package com.example.amortizacion.service;

import com.example.amortizacion.dto.AmortizationRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class ExportService {

    private static final float PAGE_WIDTH = 595f;    // A4 portrait
    private static final float PAGE_HEIGHT = 842f;
    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 15f;
    private static final float FONT_SIZE = 10f;
    private static final float TITLE_SIZE = 16f;
    private static final float HEADER_TOP_GAP = 25f;

    // The sum of the widths must fit within PAGE_WIDTH - 2 * MARGIN (515pt).
    private static final float[] COLUMN_WIDTHS = {35f, 70f, 70f, 70f, 80f, 80f, 100f};
    private static final String[] HEADERS = {"Month", "Payment", "Principal", "Interest", "Total P.", "Total Int.", "Balance"};

    public void exportToCsv(List<AmortizationRow> schedule, String loanName, OutputStream out) throws IOException {
        java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.OutputStreamWriter(out)));
        writer.println("Month,Payment,Principal,Interest,Total Principal,Total Interest,Remaining Balance");

        for (AmortizationRow row : schedule) {
            writer.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
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

    public void exportToPdf(List<AmortizationRow> schedule, String loanName, OutputStream out) throws IOException {
        PDDocument document = new PDDocument();
        PDRectangle pageSize = new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT);
        float[] columnStarts = columnStarts();
        float top = PAGE_HEIGHT - MARGIN;

        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        try {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_SIZE);
            contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, MARGIN, top));
            contentStream.showText("Amortization Schedule - " + loanName);
            contentStream.endText();

            float y = top - HEADER_TOP_GAP;
            y = drawRow(contentStream, HEADERS, columnStarts, y, true);

            for (AmortizationRow row : schedule) {
                if (y < MARGIN) {
                    // Start a new page. The content stream is bound to the page it was
                    // opened with, so it must be closed and reopened on the new page,
                    // otherwise the rows would keep being drawn on the first page.
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = top - HEADER_TOP_GAP;
                    y = drawRow(contentStream, HEADERS, columnStarts, y, true);
                }

                String[] cells = {
                    String.valueOf(row.month()),
                    String.format("%.2f", row.payment()),
                    String.format("%.2f", row.principalPaid()),
                    String.format("%.2f", row.interestPaid()),
                    String.format("%.2f", row.totalPrincipalPaid()),
                    String.format("%.2f", row.totalInterestPaid()),
                    String.format("%.2f", row.remainingBalance())
                };
                y = drawRow(contentStream, cells, columnStarts, y, false);
            }
        } finally {
            contentStream.close();
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        document.save(baos);
        document.close();

        out.write(baos.toByteArray());
        out.close();
    }

    private float[] columnStarts() {
        float[] starts = new float[COLUMN_WIDTHS.length];
        float x = MARGIN;
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            starts[i] = x;
            x += COLUMN_WIDTHS[i];
        }
        return starts;
    }

    /**
     * Draws one row with its text baseline at the given y position. The first
     * column (month) is left aligned; numeric columns are right aligned within
     * their width so values line up vertically.
     *
     * @return the y position for the next row.
     */
    private float drawRow(PDPageContentStream contentStream,
                          String[] cells, float[] columnStarts, float y, boolean isHeader)
            throws IOException {
        PDType1Font font = isHeader ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;

        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE);
        for (int i = 0; i < cells.length; i++) {
            float textWidth = font.getStringWidth(cells[i]) / 1000f * FONT_SIZE;
            float x = (i == 0)
                ? columnStarts[0]
                : columnStarts[i] + COLUMN_WIDTHS[i] - 2f - textWidth;
            contentStream.setTextMatrix(new Matrix(1, 0, 0, 1, x, y));
            contentStream.showText(cells[i]);
        }
        contentStream.endText();

        return y - ROW_HEIGHT;
    }
}
