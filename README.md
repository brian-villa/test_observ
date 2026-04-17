<div align="center">
  <img src="https://via.placeholder.com/150x150.png?text=SGMTA+Logo" alt="SGMTA Logo" width="150"/>

  # SGMTA
  **Automated Test Management and Monitoring System**

  [![English](https://img.shields.io/badge/Language-English-blue)](README.md)
  [![Português](https://img.shields.io/badge/Language-Portugu%C3%AAs-green)](README.pt.md)
</div>

---

## Objective

In modern software development, fast CI/CD pipelines are essential. However, Quality Assurance (QA) teams often struggle with the "noise" of test reports: intermittent failures (*Flaky Tests*) that break builds and erode trust in automation.

**SGMTA** was created to solve this problem. Acting as the analytical brain of your testing infrastructure, the system passively receives telemetry from your CI/CD tools (Jenkins, GitHub Actions, etc.) via M2M Integration. 

**Main Objective:** To transform raw *Pass/Fail* data into a **dynamic Health Score**, mathematically identifying which tests are genuinely defective and which are merely suffering from environmental instability, allowing your team to act exactly where it's needed.

---

## Documentation

To keep this README focused on the essentials, we have detailed the architecture and tutorials in our documentation. Check the links below:

* **[CI/CD Integration Guide](docs/en/INTEGRATION.md):** How to send data (JSON/XML) to SGMTA.
* **[Flakiness Engine](docs/en/FLAKINESS.md):** How our Sliding Window tolerance works.
* **[OpenAPI & Swagger](docs/en/OPENAPI.md):** Complete reference of the API endpoints.

---

## Main Stack

**Backend**
* Java 21 & Spring Boot 4.x (Web, Data JPA, Security)
* PostgreSQL
* Build Tool: Apache Maven

**Frontend**
* React & Vite
* Tailwind CSS v4

---

## Setup

### Prerequisites
* Java 21 & Node.js 22+
* PostgreSQL on port `5432`

### 1. Start Backend
SGMTA uses `application.yaml`:

```yaml
spring:
  application:
    name: sgmta
  profiles:
    active: dev
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASS}

api:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000 # 24 hours


# Clone the repository
git clone [https://github.com/brian-villa/test_observ](https://github.com/brian-villa/test_observ)
cd test_observ/backend

# Build and start the Spring Boot server
./mvnw clean install -DskipTests
./mvnw spring-boot:run

### 2. Start Frontend

cd ../frontend

# Create environment variable:
export VITE_API_BASE_URL=http://localhost:8080

# Install dependencies and start:
npm install
npm run dev