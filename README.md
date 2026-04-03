# Car Rental Management System

A full-stack web application for managing a car rental business, built with Spring MVC and deployed as a WAR. The system supports multiple user roles (Admin, Manager, Customer) with a complete booking lifecycle, automated invoicing, and email notifications.

## Features

- **Car Fleet Management** — add, edit, and delete vehicles; track availability (Available / Rented / Maintenance)
- **Booking System** — customers book available cars with date ranges; managers confirm and process payments
- **Automated Invoicing** — PDF invoices generated on payment with 21% VAT calculation, downloadable from the app
- **Role-Based Access Control** — Admin, Manager, and Customer roles with fine-grained permissions via Spring Security
- **Email Verification** — account activation via verification token sent to email on registration
- **reCAPTCHA** — Google reCAPTCHA v3 protection on the registration form
- **Scheduled Tasks** — background job runs every 60 seconds to auto-cancel overdue unpaid bookings
- **REST API** — JSON endpoints for booking management (`/api/bookings`)
- **Internationalization** — Polish and English language support

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Framework 6.0.11 (Web MVC, ORM, Security, AOP) |
| ORM | Hibernate 6.0 + Spring Data JPA 3.1 |
| View | Thymeleaf 3.1 |
| Database | PostgreSQL |
| Connection Pool | HikariCP 5.0 |
| Security | Spring Security 6.1 + BCrypt |
| PDF | iTextPDF 8.0 |
| Email | Jakarta Mail 2.0 (Gmail SMTP) |
| Build | Maven 3 / WAR |

## Project Structure

```
src/main/java/com/transport/
├── config/          # Spring configuration classes
├── controller/      # MVC controllers + REST API
├── dto/             # Data transfer objects
├── entity/          # JPA entities
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
src/main/resources/
├── templates/       # Thymeleaf HTML templates
├── static/          # CSS, JS, images
└── application.properties
```

## Entities

- **User** — account with role assignment and customer-manager hierarchy
- **Role** — `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_CUSTOMER`
- **Car** — vehicle with pricing and status
- **Booking** — reservation linking a customer and car with a status lifecycle (`PENDING → CONFIRMED → PAID → COMPLETED`)
- **Invoice** — financial record generated on payment
- **VerificationToken** — email verification token tied to user registration

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/bookings` | List all bookings |
| GET | `/api/bookings/{id}` | Get booking by ID |
| POST | `/api/bookings` | Create booking |
| PUT | `/api/bookings/{id}` | Update booking |
| DELETE | `/api/bookings/{id}` | Delete booking |

## Setup

### Prerequisites

- Java 17+
- Maven 3+
- PostgreSQL running locally
- A Gmail account with an App Password for email sending

### Database

Create a PostgreSQL database:

```sql
CREATE DATABASE car_rental;
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/car_rental
spring.datasource.username=postgres
spring.datasource.password=your_password

# Email (Gmail App Password)
spring.mail.username=your@gmail.com
spring.mail.password=your_app_password

# File uploads
app.upload.dir=uploads/cars

# Google reCAPTCHA (https://www.google.com/recaptcha/admin)
google.recaptcha.site-key=your_recaptcha_site_key
google.recaptcha.secret-key=your_recaptcha_secret_key
```

### Build & Run

```bash
mvn clean package
# Deploy the generated WAR to Tomcat (or use your IDE's built-in server)
```

The application will be available at `http://localhost:8080`.

## First Run Setup

### 1. Seed roles

```sql
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_MANAGER'), ('ROLE_CUSTOMER');
```

### 2. Create admin user

Registration via the form creates `ROLE_CUSTOMER` only. Insert an admin manually:

```sql
INSERT INTO users (username, email, password, first_name, last_name, enabled)
VALUES ('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'Admin', true);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
```

This creates an admin with password `admin`. Change it after first login.

---

*Academic project — Web Programming Frameworks and Tools, 2025*
