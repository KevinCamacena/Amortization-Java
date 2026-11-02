package com.example.amortizacion.repository;

import com.example.amortizacion.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByNameContainingIgnoreCase(String name);
}
