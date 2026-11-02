package com.example.amortizacion.service;

import com.example.amortizacion.entity.ExtraPayment;
import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.exception.LoanNotFoundException;
import com.example.amortizacion.repository.ExtraPaymentRepository;
import com.example.amortizacion.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final ExtraPaymentRepository extraPaymentRepository;

    public LoanService(LoanRepository loanRepository, ExtraPaymentRepository extraPaymentRepository) {
        this.loanRepository = loanRepository;
        this.extraPaymentRepository = extraPaymentRepository;
    }

    public Loan create(Loan loan) {
        return loanRepository.save(loan);
    }

    public Loan update(Long id, Loan loan) {
        Loan existing = loanRepository.findById(id)
            .orElseThrow(() -> new LoanNotFoundException(id));

        existing.setName(loan.getName());
        existing.setTotalPrice(loan.getTotalPrice());
        existing.setDownPayment(loan.getDownPayment());
        existing.setDownPaymentPercent(loan.getDownPaymentPercent());
        existing.setPrincipal(loan.getPrincipal());
        existing.setAnnualInterestRate(loan.getAnnualInterestRate());
        existing.setTermMonths(loan.getTermMonths());
        existing.syncFinancingFields();

        for (ExtraPayment ep : loan.getExtraPayments()) {
            ep.setId(null);
            ep.setLoan(existing);
            existing.addExtraPayment(ep);
        }

        return loanRepository.save(existing);
    }

    public Optional<Loan> findById(Long id) {
        Optional<Loan> loan = loanRepository.findById(id);
        loan.ifPresent(l -> l.getExtraPayments().size());
        return loan;
    }

    public Loan findByIdOrThrow(Long id) {
        return loanRepository.findById(id)
            .orElseThrow(() -> new LoanNotFoundException(id));
    }

    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    public void delete(Long id) {
        loanRepository.deleteById(id);
    }

    public Loan addExtraPayment(Long loanId, ExtraPayment extraPayment) {
        Loan loan = findByIdOrThrow(loanId);
        loan.addExtraPayment(extraPayment);
        return loanRepository.save(loan);
    }

    public Loan toggleExtraPayment(Long loanId, Long extraPaymentId) {
        Loan loan = findByIdOrThrow(loanId);
        loan.getExtraPayments().forEach(ep -> {
            if (ep.getId().equals(extraPaymentId)) {
                ep.setEnabled(!ep.getEnabled());
            }
        });
        return loanRepository.save(loan);
    }

    public Loan deleteExtraPayment(Long loanId, Long extraPaymentId) {
        Loan loan = findByIdOrThrow(loanId);
        loan.getExtraPayments().removeIf(ep -> ep.getId().equals(extraPaymentId));
        return loanRepository.save(loan);
    }
}
