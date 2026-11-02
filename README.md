# Amortization & Car Financing Calculator

A full-featured amortization and asset financing web application built with **Java 25**, **Spring Boot**, **Thymeleaf**, and **Chart.js**.

---

## Features

### 1. Root Redirection
- Visiting `http://localhost:8080/` automatically redirects to `/loans`, providing an immediate landing on the loans dashboard.

### 2. Vehicle & Purchase Financing Calculator
- **Car / Total Cost**: Input the total purchase price of a vehicle or property.
- **Flexible Down Payment / First Installment**: Set either:
  - A down payment **Percentage (%)** (e.g. 20%), or
  - A fixed dollar **Amount ($)** (e.g. \$7,000.00).
- **Auto-Calculated Principal**: Dynamically calculates the financed loan principal (`Total Cost - Down Payment`) in real time, with two-way binding between cost, down payment, and principal.
- **Direct Loan Support**: Direct entry of loan amounts remains supported for general personal loans or mortgages without down payments.

### 3. Extra Payment Flexibility
- **Recurrence Frequencies**:
  - **One-time**: Applied at a specific month.
  - **Monthly Recurring**: Automatically adds an extra principal payment every month starting from a chosen month (with optional end month).
  - **Annually Recurring**: Adds an extra principal payment once every 12 months (ideal for annual bonuses or tax refunds).
- **Active Extra Payment Management**: Easily enable, pause, or remove extra payments directly from the loan details view.
- **Payoff Acceleration Engine**: Automatically calculates early payoff date, total months saved, and total interest saved.

### 4. Interactive Visualizations (Chart.js)
- **Payoff Curve (Standard vs. Accelerated)**:
  - Direct comparison of remaining balance trajectories over time (standard scheduled balance vs. accelerated balance with extra payments).
- **Amortization Curve**:
  - Visualizes cumulative principal paid, cumulative interest paid, and remaining balance over the loan term.
- **Cost Breakdown (Donut Chart)**:
  - Interactive breakdown of loan composition showing percentages for Financed Principal, Total Interest Paid, and Down Payment.
- **Savings Impact Banner**:
  - Highlight card displaying exact time saved (e.g. *14 Months Faster*) and total interest dollars saved.

### 5. Copy & Scenario Cloning
- **One-Click "Copy" Action**: Available on both the loan list table and loan detail view.
- **Scenario Testing**: Clones all loan parameters (purchase price, down payment, interest rate, term, and extra payment schedules) into the loan form with an intuitive guide banner, allowing you to instantly tweak down payments (e.g. from 20% to 30% or 40%) and save alternative scenarios.

### 6. Side-by-Side Comparison Matrix & Trajectory Chart
- **Loan Selection**: Checkboxes on the loans list to select 2 or more loans with a dynamic `Compare Selected (X)` action button, plus an interactive loan selector chip bar on the `/loans/compare` page.
- **Combined Payoff Trajectory Chart**: Multi-line Chart.js graph overlaying the balance payoff trajectories of all compared scenarios on a single canvas.
- **Detailed Comparison Matrix**: Side-by-side table comparing:
  - Car / Total Cost ($)
  - Down Payment ($ and %)
  - Financed Principal ($)
  - Interest Rate (APR %)
  - Monthly Base Payment ($)
  - Total Interest Paid ($)
  - Payoff Time (Months)
  - Total Loan Cost and Total Out-of-Pocket Cost

### 7. Exporting
- **CSV & PDF Exports**: Download complete amortization schedules formatted with PDFBox and OpenCSV.

### 8. UI & UX Polish
- **Formatted Input Groups**: Clear prefix and suffix badges (`$`, `%`, `Mo`) for currency and duration fields.
- **Real-Time Client-Side Validation Hints**: Instant feedback on invalid inputs, high interest rates, down payment exceeding car cost, and human-readable term conversions (e.g. `60 months (~5.0 years)`).
- **Responsive Table Layouts**: Mobile-friendly table containers with sticky headers and clean status badges.

---

## Tech Stack

- **Java**: 25
- **Framework**: Spring Boot 4.1.1
- **Template Engine**: Thymeleaf
- **Data & Persistence**: Spring Data JPA, H2 In-Memory Database
- **Charting**: Chart.js 4.4.7
- **Exports**: Apache PDFBox 2.0.34, OpenCSV 5.9
- **Build Tool**: Gradle 9.7.1

---

## Getting Started

### Prerequisites
- JDK 25 installed and available on your `PATH`.

### Running the Application

#### Option A: Running with Gradle (Local)
```bash
./gradlew bootRun
```

#### Option B: Running with Docker
1. **Build the Docker image**:
   ```bash
   docker build -t amortization-app:latest .
   ```
2. **Run the container**:
   ```bash
   docker run -d --name amortization-app -p 8080:8080 amortization-app:latest
   ```

#### Option C: Running with Docker Compose
```bash
docker compose up -d
```
To view logs or stop the container:
```bash
docker compose logs -f
docker compose down
```

Access the application at: `http://localhost:8080/`

### Running the Test Suite
```bash
./gradlew test
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/amortizacion/
│   │   ├── controller/
│   │   │   ├── ExportController.java     # CSV / PDF exports and comparison view
│   │   │   ├── HomeController.java       # Root path redirection to /loans
│   │   │   └── LoanViewController.java   # Loan CRUD and extra payment handling
│   │   ├── dto/
│   │   │   ├── AmortizationRow.java      # Single row in amortization schedule
│   │   │   ├── ChartPoint.java           # Coordinate for Chart.js
│   │   │   └── PayoffComparison.java     # Accelerated vs standard payoff comparison metrics
│   │   ├── entity/
│   │   │   ├── ExtraPayment.java         # Extra payment entity with recurrence support
│   │   │   ├── Loan.java                 # Loan entity with car cost & down payment fields
│   │   │   └── RecurrenceType.java       # Enum for ONE_TIME, MONTHLY, ANNUALLY
│   │   ├── repository/                   # Spring Data JPA repositories
│   │   └── service/
│   │       ├── AmortizationService.java  # Core calculation engine & comparison logic
│   │       ├── ExportService.java        # PDF & CSV generation
│   │       └── LoanService.java          # Loan business logic
│   └── resources/
│       ├── static/css/style.css          # Modern styles, input groups, badges, tables
│       └── templates/                    # Thymeleaf templates (form, detail, list, compare)
└── test/                                 # Unit and integration test suites
```

