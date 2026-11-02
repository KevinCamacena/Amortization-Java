package com.example.amortizacion.repository;

import com.example.amortizacion.entity.ExtraPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraPaymentRepository extends JpaRepository<ExtraPayment, Long> {
    List<ExtraPayment> findByLoanId(Long loanId);
}
