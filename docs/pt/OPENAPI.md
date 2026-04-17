# Referência da API REST 

## Documentação Interativa

A forma recomendada de explorar a API é através do Swagger UI. Esta interface permite-lhe não só ler os contratos de dados, mas também executar pedidos HTTP de teste diretamente pelo browser.

1. Inicie o servidor backend localmente (`./mvnw spring-boot:run`).
2. Aceda a: **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

> **Nota sobre Autenticação no Swagger:**
> A maioria dos endpoints requer um token JWT. Para testar rotas protegidas no Swagger:
> 1. Vá ao endpoint `/api/v1/auth/login` e faça a sua requisição com as credenciais.
> 2. Copie o valor do `token` que receber na resposta.
> 3. Suba até ao topo da página do Swagger, clique no botão **"Authorize"** e cole lá o seu token.

---

##  Snapshot Estático (Integrações e Frontend)

Para facilitar a vida a equipas de Frontend que usem ferramentas de geração automática de código (como o *Swagger Codegen* ou o *Orval*), ou caso queira importar a nossa API para o Postman/Insomnia, mantemos um *snapshot* estático da versão oficial na pasta de documentação.

📄 **Ficheiro:** [`openapi_v1.json`](openapi_v1.json)

### Como atualizar o Snapshot Automático
Este ficheiro JSON **não deve ser editado à mão**. Ele é extraído magicamente do código-fonte através do plugin `springdoc-openapi-maven-plugin`.

Sempre que alterar os *Controllers* do Spring Boot e quiser atualizar este contrato, basta correr o ciclo de build completo na raiz do backend:

```bash
./mvnw clean verify -DskipTests