# Claims Adjudication Engine

A Java-based health insurance claims adjudication engine that evaluates claim lines against policy eligibility, coverage, provider network, and cost-sharing rules.

## Features

- Validate member eligibility against policy effective and termination dates.
- Check covered services and required referrals or prior authorizations.
- Apply in-network contracted rates and out-of-network usual-and-customary pricing rules.
- Process allowed amounts, remaining deductibles, copayments, and coinsurance in the required order.
- Apply the remaining out-of-pocket maximum to limit patient responsibility.
- Return adjudication results with insurer-paid amounts, patient responsibility, denial reasons, and explanations.
- Process multi-line claims in order and maintain accumulator state.
- Support duplicate-claim and timely-filing checks.
- Handle annual accumulator resets, family deductible sharing, and retroactive recovery paths covered by the implementation.

## Technology

- Java 21
- Maven Wrapper
- JUnit 5

## Prerequisites

- Install a Java 21 JDK.
- Use the Maven Wrapper included in the repository; a separate Maven installation is not required.

## Run the tests

Open PowerShell in the project root and run:

```powershell
.\mvnw.cmd clean test
```

The last recorded full test run completed with 104 tests, zero failures, zero errors, and zero skipped tests.

## Project structure

- `src/main/java/` — claim, policy, patient, provider, accumulator, and adjudication domain classes.
- `src/test/java/` — automated tests for adjudication rules and edge cases.
- `pom.xml` — Maven project configuration.

## Scope

This project implements rules-based claims adjudication. A successful test suite does not by itself establish HIPAA compliance or certification.
