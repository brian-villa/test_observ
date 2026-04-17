# Guia de Integração com um pipeline

O SGMTA é **agnóstico à sua ferramenta de CI/CD**. Ele não importa dados diretamente do seu repositório; em vez disso, o seu pipeline (Jenkins, GitHub Actions, GitLab CI) deve fazer *push* nos resultados dos testes para o SGMTA no final de cada execução.

## O Endpoint de Ingestão

Todas as integrações comunicam com um único endpoint:

* **URL:** `POST /api/v1/ingest`
* **Content-Type:** `application/json` (ou `application/xml`)

## Autenticação e Headers Obrigatórios

Para garantir o envio dos dados, o sistema necessita dos seguintes *Headers* HTTP na requisição:

|      Header       |                       Descrição                                    |              Exemplo                   |
|                   |
| `X-Project-Token` | **(Obrigatório)** A API Key gerada no Dashboard.                   | `123e4567-e89b-12d3-a456-426614174000` |
| `X-Version-Name`  | Identificador da Release ou Build no CI.                           |   `BUILD-142` ou `v1.2.0`              |
| `X-Branch-Name`   | A branch onde os testes correram.                                  | `feature/login` ou `main`              |
| `X-Suite-Name`    | Nome do grupo de testes.                                           | `Backend-E2E`                          |
| `X-Execution-Id`  | ID Único do pipeline no seu CI/CD.                                 | `1847592834`                           |

*(Dica de Ouro: Para o `X-Execution-Id`, evite usar apenas o número da Build. Concatene o ID da Build com a Tentativa (Retry) para que o sistema não confunda re-runs).*

## Exemplo GitHub Actions (Java/Maven via XML)

Se utiliza Maven, o plugin Surefire gera automaticamente relatórios XML. Pode iterar sobre esses ficheiros e enviá-los diretamente para o SGMTA. 

Adicione o seguinte passo (`step`) ao final do seu `workflow.yml`. É crucial usar `if: always()` para garantir que a métrica é enviada mesmo quando os testes falham.

```yaml
      - name: Teste
        if: always()
        env:
            SGMTA_URL: ${{ vars.SGMTA_API_URL }}
            SGMTA_TOKEN: ${{ secrets.SGMTA_PROJECT_TOKEN }}
        run: |
            BRANCH_NAME="${{ github.ref_name }}"
            VERSION_NAME="build-${{ github.run_number }}"
            RUN_ID="${{ github.run_id }}-${{ github.run_attempt }}"
            SUITE_NAME="Backend-Unit-Tests"
            
            for file in target/surefire-reports/TEST-*.xml; do
              if [ -f "$file" ]; then
                curl -X POST "$SGMTA_URL/api/v1/ingest" \
                     -H "Content-Type: application/xml" \
                     -H "X-Project-Token: $SGMTA_TOKEN" \
                     -H "X-Version-Name: $VERSION_NAME" \
                     -H "X-Branch-Name: $BRANCH_NAME" \
                     -H "X-Suite-Name: $SUITE_NAME" \
                     -H "X-Execution-Id: $RUN_ID" \
                     -d @"$file"
              fi
            done


## Exemplo Jenkinsfile (JSON) - Não testado ainda

post {
    always {
        script {
            withCredentials([string(credentialsId: 'SGMTA_API_KEY', variable: 'TOKEN')]) {
                sh """
                curl -X POST "[https://api.seu-dominio.com/api/v1/ingest](https://api.seu-dominio.com/api/v1/ingest)" \\
                     -H "Content-Type: application/json" \\
                     -H "X-Project-Token: ${TOKEN}" \\
                     -H "X-Version-Name: BUILD-${BUILD_NUMBER}" \\
                     -H "X-Branch-Name: ${GIT_BRANCH}" \\
                     -H "X-Suite-Name: Frontend-E2E" \\
                     -H "X-Execution-Id: ${BUILD_TAG}" \\
                     -d @target/test-results.json
                """
            }
        }
    }
}


## Se o seu pipeline exporta ou converte os resultados para JSON, o SGMTA espera o seguinte contrato padronizado:

```json
{
  "startTime": "2026-04-16T10:00:00Z",
  "endTime": "2026-04-16T10:05:00Z",
  "tests": [
    {
      "testName": "com.exemplo.SGMTA.shouldAuthenticateUser",
      "status": "PASS",
      "errorMessage": null
    },
    {
      "testName": "com.exemplo.SGMTA.shouldRejectInvalidToken",
      "status": "FAIL",
      "errorMessage": "Expected 401 but got 200\n at com.exemplo.SGMTA.shouldRejectInvalidToken(SGMTA.java:45)"
    }
  ]
}