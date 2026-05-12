package com.example.sgmta.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class IngestionService {

    private final ProjectRepository projectRepository;
    private final VersionService versionService;
    private final TestCaseService testCaseService;
    private final TestExecutionService testExecutionService;
    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;

    public IngestionService(ProjectRepository projectRepository, VersionService versionService,
                            TestCaseService testCaseService, TestExecutionService testExecutionService,
                            TestExecutionRepository testExecutionRepository,
                            TestResultRepository testResultRepository) {
        this.projectRepository = projectRepository;
        this.versionService = versionService;
        this.testCaseService = testCaseService;
        this.testExecutionService = testExecutionService;
        this.testExecutionRepository = testExecutionRepository;
        this.testResultRepository = testResultRepository;
    }

    @Transactional
    public void ingest(StandardizedPipelineReport report, String suiteName, String executionId, String buildName) {

        Project project = projectRepository.findByProjectToken(report.projectToken())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado para o token fornecido."));

        Version version = versionService.findOrCreate(report.versionName());

        Optional<TestExecution> existingExecOpt = testExecutionRepository
                .findTopByProjectIdAndSuiteNameAndRunId(project.getId(), suiteName, executionId);

        TestExecution execution;

        if (existingExecOpt.isPresent()) {
            execution = existingExecOpt.get();
            if (report.startTime() != null && report.endTime() != null) {
                long payloadDurationMillis = Duration.between(report.startTime(), report.endTime()).toMillis();
                if (payloadDurationMillis > 0) {
                    execution.setEndTime(execution.getEndTime().plusNanos(payloadDurationMillis * 1_000_000));
                    execution = testExecutionRepository.save(execution);
                }
            }
        } else {
            execution = testExecutionService.createExecution(
                    project, version, report.branchName(),
                    report.startTime(), report.endTime(),
                    suiteName, executionId, buildName
            );
        }

        for (StandardizedPipelineReport.TestCaseResult item : report.tests()) {
            TestCase testCase = testCaseService.findOrCreate(item.testName());

            ExtractionResult extraction = extractScreenshotAndCleanError(item.errorMessage());

            // Um único save atómico com cleanError + screenshot juntos.
            TestResult currentResult = testResultRepository.save(new TestResult(
                    item.status(),
                    false,
                    extraction.cleanError(),
                    extraction.screenshot(),
                    execution,
                    testCase
            ));

            checkAndMarkFlaky(testCase, project, currentResult);
        }
    }

    private void checkAndMarkFlaky(TestCase testCase, Project project, TestResult currentResult) {
        int threshold = project.getFlakyThreshold();

        if (threshold <= 0) {
            currentResult.setFlaky(false);
            testResultRepository.save(currentResult);
            return;
        }

        Version currentVersion = currentResult.getTestExecution().getVersion();
        List<TestResult> window;

        if (currentVersion != null) {
            window = testResultRepository.findRecentResultsByTestCaseProjectAndVersion(
                    testCase.getId(),
                    project.getId(),
                    currentVersion.getId(),
                    PageRequest.of(0, threshold, Sort.by(Sort.Direction.DESC, "testExecution.startTime"))
            ).getContent();
        } else {
            window = testResultRepository.findRecentResultsByTestCaseAndProject(
                    testCase.getId(),
                    project.getId(),
                    PageRequest.of(0, threshold, Sort.by(Sort.Direction.DESC, "testExecution.startTime"))
            ).getContent();
        }

        if (window.size() < threshold) {
            currentResult.setFlaky(false);
            testResultRepository.save(currentResult);
            return;
        }

        long passCount = window.stream().filter(r -> "PASS".equalsIgnoreCase(r.getResult())).count();
        long failCount = window.stream().filter(r -> "FAIL".equalsIgnoreCase(r.getResult())).count();

        currentResult.setFlaky(passCount > 0 && failCount > 0);
        testResultRepository.save(currentResult);
    }

    private record ExtractionResult(String cleanError, String screenshot) {}

    /**
     * Extração agnóstica de imagens!
     * Procura automaticamente por padrões de Base64 (JPEG ou PNG) dentro do log de erro,
     * ignorando quebras de linha introduzidas pelo XML.
     */
    private ExtractionResult extractScreenshotAndCleanError(String rawError) {
        if (rawError == null || rawError.isBlank()) {
            return new ExtractionResult(null, null);
        }

        String cleanError = rawError;
        String screenshot = null;

        // REGEX MAGIA: Procura por uma string que comece por assinatura JPEG (/9j/) ou PNG (iVBORw0)
        // O \\r\\n permite que o Regex salte as quebras de linha do XML.
        // Ele vai PARAR automaticamente quando encontrar um espaço (ex: " at com.example...")
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:/9j/|iVBORw0KGgo)[A-Za-z0-9+/=\\r\\n]{500,}");
        java.util.regex.Matcher matcher = pattern.matcher(cleanError);

        if (matcher.find()) {
            // Extrai a imagem e remove TODAS as quebras de linha para o HTML renderizar perfeitamente
            screenshot = matcher.group(0).replaceAll("[\\r\\n]", "");

            // Remove o Base64 gigante do log para a interface ficar limpa
            cleanError = cleanError.replace(matcher.group(0), "[EVIDENCIA_VISUAL_EXTRAIDA]");
        }

        // Limpeza do formato {message=..., type=..., =STACKTRACE} do JUnit/Surefire
        if (cleanError.startsWith("{") && cleanError.contains("message=")) {
            try {
                int msgStart = cleanError.indexOf("message=") + 8;
                int stackStart = cleanError.indexOf(", =");
                int typeStart  = cleanError.indexOf(", type=");

                int msgEnd = cleanError.length();
                if (typeStart > msgStart) msgEnd = Math.min(msgEnd, typeStart);

                String messageField = cleanError.substring(msgStart, msgEnd).trim();
                if (messageField.endsWith(",")) messageField = messageField.substring(0, messageField.length() - 1).trim();

                String typeField = "";
                if (typeStart >= 0) {
                    int typeValueStart = typeStart + 7;
                    int typeValueEnd = stackStart > typeStart ? stackStart : cleanError.length();
                    if (typeValueEnd > typeValueStart) {
                        typeField = cleanError.substring(typeValueStart, typeValueEnd).trim();
                        if (typeField.endsWith(",")) typeField = typeField.substring(0, typeField.length() - 1).trim();
                    }
                }

                StringBuilder sb = new StringBuilder();
                if (!messageField.isBlank()) sb.append(messageField);
                if (!typeField.isBlank())    sb.append("\nType: ").append(typeField);

                if (stackStart >= 0) {
                    String stackPart = cleanError.substring(stackStart + 3);
                    if (stackPart.endsWith("}")) stackPart = stackPart.substring(0, stackPart.length() - 1);
                    if (!stackPart.isBlank()) sb.append("\n\nStack Trace:\n").append(stackPart.trim());
                }
                cleanError = sb.toString().trim();
            } catch (Exception ignored) {
                cleanError = cleanError.replace("[EVIDENCIA_VISUAL_EXTRAIDA]", "");
            }
        }

        return new ExtractionResult(cleanError.trim(), screenshot);
    }
}