# Pipeline Integration Guide

SGMTA is **agnostic to your CI/CD tool**. It does not import data directly from your repository; instead, your pipeline (Jenkins, GitHub Actions, GitLab CI) must *push* the test results to SGMTA at the end of each execution.

## The Ingestion Endpoint

All integrations communicate with a single endpoint:

* **URL:** `POST /api/v1/ingest`
* **Content-Type:** `application/json` (or `application/xml`)

## Authentication and Mandatory Headers

To ensure data delivery, the system requires the following HTTP Headers in the request:

| Header | Description | Example |
| :--- | :--- | :--- |
| `X-Project-Token` | **(Mandatory)** The API Key generated in the Dashboard. | `123e4567-e89b-12d3-a456-426614174000` |
| `X-Version-Name` | Release or Build identifier in the CI. | `BUILD-142` or `v1.2.0` |
| `X-Branch-Name` | The branch where the tests ran. | `feature/login` or `main` |
| `X-Suite-Name` | Test suite name. | `Backend-E2E` |
| `X-Execution-Id` | Unique pipeline ID in your CI/CD. | `1847592834` |

*(Pro Tip: For the `X-Execution-Id`, avoid using just the Build number. Concatenate the Build ID with the Attempt/Retry so the system doesn't confuse re-runs).*

## GitHub Actions Example (Java/Maven via XML)

If you use Maven, the Surefire plugin automatically generates XML reports. You can iterate over these files and send them directly to SGMTA. 

Add the following step at the end of your `workflow.yml`. It is crucial to use `if: always()` to ensure the metric is sent even when tests fail.

```yaml
      - name: Send Test Results
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

## Jenkinsfile Example (JSON) - Not tested yet

post {
    always {
        script {
            withCredentials([string(credentialsId: 'SGMTA_API_KEY', variable: 'TOKEN')]) {
                sh """
                curl -X POST "[https://api.your-domain.com/api/v1/ingest](https://api.your-domain.com/api/v1/ingest)" \\
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

## JSON Payload Format
If your pipeline exports or converts results to JSON, SGMTA expects the following standardized contract:

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