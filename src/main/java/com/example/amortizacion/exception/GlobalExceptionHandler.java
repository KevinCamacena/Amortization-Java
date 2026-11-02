package com.example.amortizacion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanNotFoundException.class)
    public String handleNotFound(LoanNotFoundException ex, org.springframework.ui.Model model) {
        model.addAttribute("errorMessage", "Loan not found: " + ex.getId());
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException ex, org.springframework.ui.Model model) {
        model.addAttribute("errorMessage", "Bad request: " + ex.getMessage());
        return "error/400";
    }
}
