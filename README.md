# SGMTA - Sistema de Gestão e Monitorização de Testes Automatizados

O **SGMTA** é uma plataforma em desenvolvimento para centralizar a gestão e a monitorização de testes automatizados. O sistema atua como o elo de ligação entre equipas de qualidade (QA) e pipelines de integração contínua (CI/CD), permitindo a ingestão, armazenamento e análise de execuções de testes via Integração Machine-to-Machine (M2M) utilizando tokens estáticos.

## Índice

1. [Stack Tecnológica](#stack-tecnológica)
2. [Módulos Implementados](#módulos-implementados)
3. [Pré-requisitos](#pré-requisitos)
4. [Configuração do Ambiente](#configuração-do-ambiente)
5. [Instruções de Execução](#instruções-de-execução)
6. [Documentação da API](#documentação-da-api)

---

## Stack Tecnológica

A arquitetura baseia-se nas seguintes tecnologias e frameworks:

* **Linguagem:** [Java 21](https://adoptium.net/)
* **Framework:** [Spring Boot 4](https://spring.io/projects/spring-boot) (Web, Data JPA, Validation)
* **Segurança:** [Spring Security](https://spring.io/projects/spring-security) com [JSON Web Tokens (JWT)](https://jwt.io/) e BCrypt
* **Base de Dados:** [PostgreSQL](https://www.postgresql.org/)
* **Gestão de Dependências e Build:** [Apache Maven](https://maven.apache.org/)
* **Especificação de API:** [OpenAPI 3 / Swagger](https://swagger.io/specification/)

---

## Módulos Implementados

O desenvolvimento segue uma abordagem modular baseada no Documento de Especificação de Requisitos (SRS). As seguintes funcionalidades encontram-se concluídas:

### 1. Autenticação e Gestão de Utilizadores
* Registo de utilizadores com encriptação de credenciais.
* Geração de tokens JWT stateless para controlo de sessão.
* Proteção de rotas através de filtros de segurança (`SecurityFilter`).

### 2. Gestão de Projetos e Credenciais 
* Operações CRUD completas para a entidade `Project`.
* Associação de equipas de desenvolvimento aos projetos.
* Geração automática de chaves de integração (API Keys) com prefixo dinâmico baseado no nome do projeto para garantir rastreabilidade.
* Rotação de chaves de segurança para invalidação de credenciais comprometidas em pipelines CI/CD.
* Isolamento da camada de apresentação através de DTOs mapeados manualmente (Padrão Mapper).
* Configuração de tolerância de falhas (*Flaky Threshold*) isolada por projeto.

### 3. Dicionários de Suporte para Ingestão (Catálogos)
* Estruturação das entidades independentes `Version` e `TestCase` para atuarem como dicionários únicos do sistema.
* Implementação do padrão *Find or Create* na camada de serviço para garantir integridade e evitar duplicação de dados enviados pelos pipelines (ex: Jenkins, GitHub Actions).
* Preparação da estrutura lógica para deteção automática de instabilidade de testes (*Flaky Tests*).
* Exposição de catálogo de leitura (*Read-Only*) via `VersionController` e `TestCaseController` para suporte futuro aos filtros do Dashboard.

---

## Pré-requisitos

Para compilar e executar o projeto no ambiente local, é necessário assegurar a instalação dos seguintes componentes:

1. **Java Development Kit (JDK):** Versão 21.
2. **Apache Maven:** Para resolução de dependências e execução do ciclo de build.
3. **PostgreSQL:** Servidor de base de dados relacional a escutar na porta `5432`.

---

## Configuração do Ambiente

O sistema depende de variáveis de ambiente para garantir o isolamento de credenciais. O ficheiro `src/main/resources/application.yaml` está configurado para injetar estas variáveis. 

Configure o seu ambiente local de acordo com o seguinte mapeamento:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sgmta_db
    username: ${DB_USER:postgres}                  
    password: ${DB_PASSWORD:admin}                 
  jpa:
    hibernate:
      ddl-auto: update                             

api:
  security:
    token:
      secret: ${JWT_SECRET:chave-secreta-default-para-ambiente-de-desenvolvimento} 
```

---

## Instruções de Execução

Siga os passos abaixo para iniciar a aplicação localmente utilizando a interface de linha de comandos (CLI):

1. **Clonar o repositório e navegar para a diretoria raiz:**
   ```bash
   git clone https://github.com/brian-villa/test_observ
   cd sgmta
   ```

2. **Limpar a build anterior, compilar o projeto e descarregar dependências:**
   ```bash
   mvn clean install
   ```

3. **Iniciar o servidor embebido do Spring Boot (Tomcat):**
   ```bash
   mvn spring-boot:run
   ```
   A aplicação será iniciada e ficará a escutar pedidos HTTP na porta `8080`.

---

## Documentação da API

Toda a superfície da API está rigorosamente documentada utilizando a especificação OpenAPI. O mapeamento inclui esquemas de validação, códigos de resposta HTTP e requisitos de segurança.

Após rodar o servidor, a documentação interativa pode ser acedida através do browser:

* **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

**Nota sobre testes interativos:** Para interagir com os endpoints protegidos diretamente na interface do Swagger, é necessário realizar a autenticação em `/login`, copiar o token JWT fornecido na resposta e inseri-lo no campo de autorização global (botão "Authorize" no topo do ecrã).
