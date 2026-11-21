# NSU RDS 2026 — Module 20: Administration & System Monitoring

**Course:** CSE215 — Programming Language II (Fall 2026)  
**Department:** Electrical & Computer Engineering (ECE), North South University (NSU)  
**Instructor:** Prof. Miftahur Rahman, Ph.D.  
**Teaching Assistant:** Nowshin Tabassum  
**Student:** Sakhawat Rahman Anan (ID: `2614160642`) — Solo Contributor  
**Package:** `edu.nsu.rds.administration`  

---

## Overview
Module 20 (Administration & System Monitoring) serves as the operational management hub for the NSU Registration & Degree System (RDS) platform.

### Core Tables & Responsibilities
- **`system_parameters`:** Central runtime system configuration (`registration.open`, `semester.current`, `maintenance_mode`, `max_login_attempts`, `support_email`).
- **`academic_calendar`:** University milestone dates and academic deadlines (`START`, `DEADLINE`, `HOLIDAY`, `EXAM`, `END`) bound to semester terms.
- **`monitoring_samples`:** Real-time operational telemetry capturing concurrent user loads, database latency, and cumulative error counts.

---

## Tech Stack
- **Language:** Java 21 (OpenJDK)
- **GUI Toolkit:** JavaFX 21
- **Database:** PostgreSQL 18 (with automatic in-memory fallback)
- **Build System:** Apache Maven

---

## Database Configuration
1. Database name: `nsu_rds_db` on `localhost:5432`.
2. Official scripts in `database/`:
   - `database/20_administration.sql` (schema & indexes)
   - `database/20_administration_data.sql` (canonical seed data)
3. Credentials configured in `src/main/resources/db.properties`.

---

## How to Run

### Run Application
```bash
mvn clean javafx:run
```

### Run Tests (16/16 JUnit 5 Tests)
```bash
mvn test
```
