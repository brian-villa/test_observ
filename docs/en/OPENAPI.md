# REST API Reference

## Interactive Documentation

The recommended way to explore the API is through Swagger UI. This interface allows you not only to read the data contracts but also to execute test HTTP requests directly from the browser.

1. Start the backend server locally (`./mvnw spring-boot:run`).
2. Access: **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

> **Note on Swagger Authentication:**
> Most endpoints require a JWT token. To test protected routes in Swagger:
> 1. Go to the `/api/v1/auth/login` endpoint and make your request with the credentials.
> 2. Copy the `token` value received in the response.
> 3. Scroll to the top of the Swagger page, click the **"Authorize"** button, and paste your token there.

---

## Static Snapshot (Integrations and Frontend)

To make life easier for Frontend teams using automatic code generation tools (like *Swagger Codegen* or *Orval*), or if you want to import our API into Postman/Insomnia, we keep a static *snapshot* of the official version in the documentation folder.

📄 **File:** [`openapi_v1.json`](openapi_v1.json)

### How to Update the Automatic Snapshot
This JSON file **must not be edited manually**. It is magically extracted from the source code via the `springdoc-openapi-maven-plugin`.

Whenever you change the Spring Boot *Controllers* and want to update this contract, simply run the complete build cycle at the root of the backend:

```bash
./mvnw clean verify -DskipTests