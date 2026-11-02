package com.example.amortizacion.controller;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.ChartPoint;
import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.AmortizationService;
import com.example.amortizacion.service.ExportService;
import com.example.amortizacion.service.LoanService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public void exportCsv(@PathVariable Long id, @RequestParam(value = "includeExtra", defaultValue = "true") boolean includeExtra,
                          HttpServletResponse response) throws IOException {
        Loan loan = loanService.findByIdOrThrow(id);
        List<AmortizationRow> schedule = amortizationService.generateSchedule(loan, includeExtra);

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_" + loan.getName() + ".csv\"");
        exportService.exportToCsv(schedule, loan.getName(), response.getOutputStream());
    }

    @GetMapping("/loans/{id}/export/pdf")
    public void exportPdf(@PathVariable Long id, @RequestParam(value = "includeExtra", defaultValue = "true") boolean includeExtra,
                          HttpServletResponse response) throws IOException {
        Loan loan = loanService.findByIdOrThrow(id);
        List<AmortizationRow> schedule = amortizationService.generateSchedule(loan, includeExtra);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amortization_" + loan.getName() + ".pdf\"");
        exportService.exportToPdf(schedule, loan.getName(), response.getOutputStream());
    }

    @GetMapping("/loans/compare")
    public String compare(@RequestParam("loanIds") List<Long> loanIds, Model model) {
        List<Loan> loans = loanIds.stream()
            .map(id -> loanService.findByIdOrThrow(id))
            .collect(Collectors.toList());

        List<AmortizationService.LoanCompareSummary> summaries = loans.stream()
            .map(loan -> amortizationService.generateCompareSummary(loan, true))
            .collect(Collectors.toList());

        model.addAttribute("loans", loans);
        model.addAttribute("summaries", summaries);
        return "loans/compare";
    }
}
