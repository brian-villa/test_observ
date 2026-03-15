# SGMTA - Sistema de Gestão e Monitorização de Testes Automatizados

O **SGMTA** é uma plataforma de backend desenvolvida para centralizar a gestão e a monitorização de testes automatizados. O sistema atua como o elo de ligação entre equipas de qualidade (QA) e pipelines de integração contínua (CI/CD), permitindo a ingestão, armazenamento e análise de execuções de testes via Integração Machine-to-Machine (M2M) utilizando tokens estáticos.

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

* **Linguagem:** [Java 17](https://adoptium.net/)
* **Framework:** [Spring Boot 3](https://spring.io/projects/spring-boot) (Web, Data JPA, Validation)
* **Segurança:** [Spring Security](https://spring.io/projects/spring-security) com [JSON Web Tokens (JWT)](https://jwt.io/) e BCrypt
* **Base de Dados:** [PostgreSQL](https://www.postgresql.org/)
* **Gestão de Dependências e Build:** [Apache Maven](https://maven.apache.org/)
* **Especificação de API:** [OpenAPI 3 / Swagger](https://swagger.io/specification/)

---

## Módulos Implementados

O desenvolvimento segue uma abordagem modular baseada no Documento de Especificação de Requisitos (SRS). As seguintes funcionalidades encontram-se concluídas:

### 1. Autenticação e Gestão de Utilizadores (RF.01, RF.02)
* Registo de utilizadores com encriptação de credenciais.
* Geração de tokens JWT stateless para controlo de sessão.
* Proteção de rotas através de filtros de segurança (`SecurityFilter`).

### 2. Gestão de Projetos e Credenciais (RF.03)
* Operações CRUD completas para a entidade `Project`.
* Associação de equipas de desenvolvimento aos projetos (Relacionamento Many-to-Many otimizado com `Set`).
* Geração automática de chaves de integração (API Keys) com prefixo dinâmico baseado no nome do projeto para garantir rastreabilidade.
* Rotação de chaves de segurança (Key Rotation) para invalidação de credenciais comprometidas em pipelines CI/CD.
* Isolamento da camada de apresentação através de DTOs mapeados manualmente (Padrão Mapper).

---

## Pré-requisitos

Para compilar e executar o projeto no ambiente local, é necessário assegurar a instalação dos seguintes componentes:

1. **Java Development Kit (JDK):** Versão 17 ou superior.
2. **Apache Maven:** Para resolução de dependências e execução do ciclo de build.
3. **PostgreSQL:** Servidor de base de dados relacional a escutar na porta `5432`.

---

## Configuração do Ambiente

O sistema depende de variáveis de ambiente para garantir o isolamento de credenciais. O ficheiro `src/main/resources/application.yml` está configurado para injetar estas variáveis. 

Certifique-se de configurar o seu ambiente local de acordo com o seguinte mapeamento:

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
   git clone <url-do-repositorio>
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

Após o arranque bem-sucedido da aplicação, a documentação interativa pode ser acedida através do browser:

* **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

**Nota sobre testes interativos:** Para interagir com os endpoints protegidos diretamente na interface do Swagger, é necessário realizar a autenticação em `/login`, copiar o token JWT fornecido na resposta e inseri-lo no campo de autorização global (botão "Authorize" no topo do ecrã).
