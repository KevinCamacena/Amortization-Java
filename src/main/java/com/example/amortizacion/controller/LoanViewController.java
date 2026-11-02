package com.example.amortizacion.controller;

import com.example.amortizacion.dto.AmortizationRow;
import com.example.amortizacion.dto.ChartPoint;
import com.example.amortizacion.dto.PayoffComparison;
import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.entity.RecurrenceType;
import com.example.amortizacion.service.AmortizationService;
import com.example.amortizacion.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/loans")
public class LoanViewController {

    private final LoanService loanService;
    private final AmortizationService amortizationService;

    public LoanViewController(LoanService loanService, AmortizationService amortizationService) {
        this.loanService = loanService;
        this.amortizationService = amortizationService;
    }

    @GetMapping
    public String list(Model model) {
        List<Loan> loans = loanService.findAll();
        model.addAttribute("loans", loans);
        return "loans/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("loan", new Loan());
        model.addAttribute("extraPayment", new ExtraPayment());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "loans/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Loan loan,
                         BindingResult result,
                         @RequestParam(value = "extraPaymentMonth", required = false) Integer month,
                         @RequestParam(value = "extraPaymentAmount", required = false) Double amount,
                         @RequestParam(value = "extraPaymentRecurrence", required = false, defaultValue = "ONE_TIME") RecurrenceType recurrence,
                         RedirectAttributes redirectAttrs,
                         Model model) {
        loan.syncFinancingFields();
        if (result.hasErrors()) {
            boolean onlyPrincipalNullError = result.getErrorCount() == 1 &&
                    result.hasFieldErrors("principal") &&
                    loan.getPrincipal() != null &&
                    loan.getPrincipal() > 0;
            if (!onlyPrincipalNullError) {
                model.addAttribute("recurrenceTypes", RecurrenceType.values());
                return "loans/form";
            }
        }

        loanService.create(loan);

        if (month != null && amount != null && month > 0 && amount > 0) {
            ExtraPayment extraPayment = new ExtraPayment();
            extraPayment.setMonthNumber(month);
            extraPayment.setAmount(amount);
            extraPayment.setRecurrenceType(recurrence != null ? recurrence : RecurrenceType.ONE_TIME);
            extraPayment.setEnabled(true);
            loanService.addExtraPayment(loan.getId(), extraPayment);
        }

        return "redirect:/loans/" + loan.getId();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Loan loan = loanService.findByIdOrThrow(id);
        model.addAttribute("loan", loan);
        model.addAttribute("extraPayment", new ExtraPayment());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "loans/form";
    }

    @GetMapping("/{id}/copy")
    public String copyForm(@PathVariable Long id, Model model) {
        Loan original = loanService.findByIdOrThrow(id);

        Loan copy = new Loan();
        copy.setName(original.getName() + " (Copy)");
        copy.setTotalPrice(original.getTotalPrice());
        copy.setDownPayment(original.getDownPayment());
        copy.setDownPaymentPercent(original.getDownPaymentPercent());
        copy.setPrincipal(original.getPrincipal());
        copy.setAnnualInterestRate(original.getAnnualInterestRate());
        copy.setTermMonths(original.getTermMonths());

        if (original.getExtraPayments() != null) {
            for (ExtraPayment ep : original.getExtraPayments()) {
                ExtraPayment epCopy = new ExtraPayment();
                epCopy.setMonthNumber(ep.getMonthNumber());
                epCopy.setEndMonth(ep.getEndMonth());
                epCopy.setAmount(ep.getAmount());
                epCopy.setRecurrenceType(ep.getRecurrenceType());
                epCopy.setEnabled(ep.getEnabled());
                copy.addExtraPayment(epCopy);
            }
        }

        model.addAttribute("loan", copy);
        model.addAttribute("extraPayment", new ExtraPayment());
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        model.addAttribute("isCopy", true);
        model.addAttribute("copiedFromName", original.getName());
        return "loans/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute Loan loan,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {
        loan.syncFinancingFields();
        if (result.hasErrors()) {
            boolean onlyPrincipalNullError = result.getErrorCount() == 1 &&
                    result.hasFieldErrors("principal") &&
                    loan.getPrincipal() != null &&
                    loan.getPrincipal() > 0;
            if (!onlyPrincipalNullError) {
                model.addAttribute("recurrenceTypes", RecurrenceType.values());
                return "loans/form";
            }
        }

        loanService.update(id, loan);
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        loanService.delete(id);
        return "redirect:/loans";
    }

    @PostMapping("/{id}/extra-payment")
    public String addExtraPayment(@PathVariable Long id,
                                  @RequestParam("monthNumber") Integer monthNumber,
                                  @RequestParam(value = "endMonth", required = false) Integer endMonth,
                                  @RequestParam("amount") Double amount,
                                  @RequestParam(value = "recurrenceType", defaultValue = "ONE_TIME") RecurrenceType recurrenceType,
                                  RedirectAttributes redirectAttrs) {
        if (monthNumber != null && amount != null && monthNumber > 0 && amount > 0) {
            ExtraPayment extraPayment = new ExtraPayment();
            extraPayment.setMonthNumber(monthNumber);
            extraPayment.setEndMonth(endMonth != null && endMonth >= monthNumber ? endMonth : null);
            extraPayment.setAmount(amount);
            extraPayment.setRecurrenceType(recurrenceType != null ? recurrenceType : RecurrenceType.ONE_TIME);
            extraPayment.setEnabled(true);
            loanService.addExtraPayment(id, extraPayment);
            redirectAttrs.addFlashAttribute("success", "Extra payment added successfully (" + extraPayment.getFormattedSchedule() + ")");
        }
        return "redirect:/loans/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(value = "includeExtra", defaultValue = "true") boolean includeExtra,
                         Model model) {
        Loan loan = loanService.findByIdOrThrow(id);
        List<AmortizationRow> schedule = amortizationService.generateSchedule(loan, includeExtra);
        List<ChartPoint> chartData = amortizationService.generateChartData(loan, includeExtra);
        List<ChartPoint> standardChartData = amortizationService.generateChartData(loan, false);
        List<ChartPoint> acceleratedChartData = amortizationService.generateChartData(loan, true);
        PayoffComparison payoffComparison = amortizationService.calculatePayoffComparison(loan);

        model.addAttribute("loan", loan);
        model.addAttribute("schedule", schedule);
        model.addAttribute("chartData", chartData);
        model.addAttribute("standardChartData", standardChartData);
        model.addAttribute("acceleratedChartData", acceleratedChartData);
        model.addAttribute("payoffComparison", payoffComparison);
        model.addAttribute("includeExtra", includeExtra);
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "loans/detail";
    }

    @PostMapping("/{id}/toggle-extra-payment")
    public String toggleExtraPayment(@PathVariable Long id,
                                     @RequestParam("extraPaymentId") Long extraPaymentId) {
        loanService.toggleExtraPayment(id, extraPaymentId);
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/remove-extra-payment")
    public String removeExtraPayment(@PathVariable Long id,
                                     @RequestParam("extraPaymentId") Long extraPaymentId) {
        loanService.deleteExtraPayment(id, extraPaymentId);
        return "redirect:/loans/" + id;
    }
}
