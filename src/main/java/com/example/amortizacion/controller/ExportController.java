package com.example.amortizacion.controller;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.AmortizationService;
import com.example.amortizacion.service.ExportService;
import com.example.amortizacion.service.LoanService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ExportController {

    private final LoanService loanService;
    private final AmortizationService amortizationService;
    private final ExportService exportService;

    public ExportController(LoanService loanService, AmortizationService amortizationService, ExportService exportService) {
        this.loanService = loanService;
        this.amortizationService = amortizationService;
        this.exportService = exportService;
    }

    @GetMapping("/loans/{id}/export/csv")
    public void exportCsv(@PathVariable Long id,
                          @RequestParam(value = "includeExtra", defaultValue = "true") boolean includeExtra,
                          HttpServletResponse response) throws IOException {
        Loan loan = loanService.findByIdOrThrow(id);
        List<AmortizationRow> schedule = amortizationService.generateSchedule(loan, includeExtra);

        response.setContentType("text/csv");
        String safeName = loan.getName() != null ? loan.getName().replaceAll("[^a-zA-Z0-9_-]", "_") : "loan";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_" + safeName + ".csv\"");
        exportService.exportToCsv(schedule, loan.getName(), response.getOutputStream());
    }

    @GetMapping("/loans/{id}/export/pdf")
    public void exportPdf(@PathVariable Long id,
                          @RequestParam(value = "includeExtra", defaultValue = "true") boolean includeExtra,
                          HttpServletResponse response) throws IOException {
        Loan loan = loanService.findByIdOrThrow(id);
        List<AmortizationRow> schedule = amortizationService.generateSchedule(loan, includeExtra);
        PayoffComparison payoffComparison = amortizationService.calculatePayoffComparison(loan);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String safeName = loan.getName() != null ? loan.getName().replaceAll("[^a-zA-Z0-9_-]", "_") : "loan";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_" + safeName + ".pdf\"");
        exportService.exportLoanToPdf(loan, schedule, payoffComparison, includeExtra, response.getOutputStream());
    }

    @GetMapping("/loans/compare")
    public String compare(@RequestParam(value = "loanIds", required = false) List<Long> loanIds, Model model) {
        List<Loan> allLoans = loanService.findAll();
        model.addAttribute("allLoans", allLoans);

        List<Loan> selectedLoans = resolveSelectedLoans(loanIds, allLoans);

        List<AmortizationService.LoanCompareSummary> summaries = selectedLoans.stream()
            .map(loan -> amortizationService.generateCompareSummary(loan, true))
            .collect(Collectors.toList());

        List<Long> selectedIds = selectedLoans.stream().map(Loan::getId).collect(Collectors.toList());

        model.addAttribute("loans", selectedLoans);
        model.addAttribute("summaries", summaries);
        model.addAttribute("selectedIds", selectedIds);
        return "loans/compare";
    }

    @GetMapping("/loans/compare/export/pdf")
    public void exportComparePdf(@RequestParam(value = "loanIds", required = false) List<Long> loanIds,
                                 HttpServletResponse response) throws IOException {
        List<Loan> allLoans = loanService.findAll();
        List<Loan> selectedLoans = resolveSelectedLoans(loanIds, allLoans);

        List<AmortizationService.LoanCompareSummary> summaries = selectedLoans.stream()
            .map(loan -> amortizationService.generateCompareSummary(loan, true))
            .collect(Collectors.toList());

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan_comparison_report.pdf\"");
        exportService.exportComparisonPdf(summaries, response.getOutputStream());
    }

    private List<Loan> resolveSelectedLoans(List<Long> loanIds, List<Loan> allLoans) {
        if (loanIds != null && !loanIds.isEmpty()) {
            return loanIds.stream()
                .filter(id -> allLoans.stream().anyMatch(l -> l.getId().equals(id)))
                .map(loanService::findByIdOrThrow)
                .collect(Collectors.toList());
        } else if (allLoans.size() >= 2) {
            return allLoans.stream().limit(3).collect(Collectors.toList());
        } else {
            return allLoans;
        }
    }
}
