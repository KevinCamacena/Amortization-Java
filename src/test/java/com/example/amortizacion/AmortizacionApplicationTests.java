package com.example.amortizacion;

import com.example.amortizacion.entity.Loan;
import com.example.amortizacion.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AmortizacionApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private LoanService loanService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void newLoanPageRendersWithoutErrors() throws Exception {
		mockMvc.perform(get("/loans/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("loans/form"))
				.andExpect(model().attributeExists("loan"));
	}

	@Test
	void createNewLoanSuccessfullyAndRedirects() throws Exception {
		mockMvc.perform(post("/loans")
				.param("name", "Subaru Crosstrek")
				.param("totalPrice", "30000")
				.param("downPaymentPercent", "20")
				.param("annualInterestRate", "5.0")
				.param("termMonths", "60"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void copyLoanPageRendersWithoutErrors() throws Exception {
		Loan loan = new Loan();
		loan.setName("Test Auto Loan");
		loan.setTotalPrice(25000.0);
		loan.setDownPaymentPercent(20.0);
		loan.setPrincipal(20000.0);
		loan.setAnnualInterestRate(6.0);
		loan.setTermMonths(48);
		loanService.create(loan);

		mockMvc.perform(get("/loans/" + loan.getId() + "/copy"))
				.andExpect(status().isOk())
				.andExpect(view().name("loans/form"))
				.andExpect(model().attribute("isCopy", true));
	}

	@Test
	void comparePageRendersWithoutErrors() throws Exception {
		mockMvc.perform(get("/loans/compare"))
				.andExpect(status().isOk())
				.andExpect(view().name("loans/compare"));
	}

	@Test
	void loansListPageRendersWithoutErrors() throws Exception {
		mockMvc.perform(get("/loans"))
				.andExpect(status().isOk())
				.andExpect(view().name("loans/list"));
	}
}
