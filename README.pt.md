<div align="center">
  <img src="https://via.placeholder.com/150x150.png?text=SGMTA+Logo" alt="SGMTA Logo" width="150"/>

  # SGMTA
  **Sistema de Gestão e Monitorização de Testes Automatizados**

  [![English](https://img.shields.io/badge/Language-English-blue)](README.md)
  [![Português](https://img.shields.io/badge/Language-Portugu%C3%AAs-green)](README.pt.md)
</div>

---

## Objetivo

No desenvolvimento de software moderno, pipelines de CI/CD rápidos são essenciais. No entanto, as equipas de Qualidade (QA) frequentemente debatem-se com o "ruído" dos relatórios de testes: falhas intermitentes (*Flaky Tests*) que quebram builds e corroem a confiança na automação.

O **SGMTA** nasce para resolver este problema. Atuando como o cérebro analítico da sua infraestrutura de testes, o sistema recebe passivamente a telemetria das suas ferramentas de CI/CD (Jenkins, GitHub Actions, etc.) via Integração M2M. 

**O Objetivo Principal:** Transformar dados brutos de *Pass/Fail* num **Health Score dinâmico**, identificando matematicamente quais os testes que estão genuinamente com defeito e quais os que sofrem apenas de instabilidade ambiental, permitindo que a sua equipa atue exatamente onde é necessário.

---

## Documentação

Para manter este README focado no essencial, detalhámos a arquitetura e os tutoriais na nossa documentação. Consulte os links abaixo:

* **[Guia de Integração CI/CD](docs/pt/INTEGRACAO.md):** Como enviar dados (JSON/XML) para o SGMTA.
* **[Motor de Flakiness](docs/pt/FLAKINESS.md):** Como funciona a nossa Janela Deslizante de tolerância.
* **[OpenAPI & Swagger](docs/pt/OPENAPI.md):** Referência completa dos endpoints da API.

---

## Stack Principal

**Backend**
* Java 21 & Spring Boot 4.x (Web, Data JPA, Security)
* PostgreSQL
* Build Tool: Apache Maven

**Frontend**
* React & Vite
* Tailwind CSS v4

---

## Configuração

### Pré-requisitos
* Java 21 & Node.js 22+
* PostgreSQL na porta `5432`

### 1. Iniciar Backend
O SGMTA utiliza o `application.yaml`:

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
      expiration: 86400000 #24 horas

```bash
# Clone o repositório
git clone [https://github.com/brian-villa/test_observ](https://github.com/brian-villa/test_observ)
cd test_observ/backend

# Compile e inicie o servidor Spring Boot
./mvnw clean install -DskipTests
./mvnw spring-boot:run

### 2. Iniciar Frontend
cd ../frontend

# Criar variável de ambiente:

VITE_API_BASE_URL=http://localhost:8080

# Instalar dependênciar e iniciar:
npm install
npm run dev