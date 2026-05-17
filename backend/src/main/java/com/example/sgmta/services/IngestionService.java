package com.example.sgmta.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.entities.enums.TestStatus;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IngestionService {

    /** Pré-compilado */
    private static final Pattern SCREENSHOT_PATTERN =
            Pattern.compile("(?:/9j/|iVBORw0KGgo)[A-Za-z0-9+/=\\r\\n]{500,}");

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
    @Operation(summary = "Processa o pipeline e ingere os resultados", description = "Extrai os logs, calcula a instabilidade (flaky) com janela deslizante e persiste os resultados.")
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
            TestCase testCase = testCaseService.findOrCreate(item.testName(), project.getId());
            ExtractionResult extraction = extractScreenshotAndCleanError(item.errorMessage());

            boolean isFlaky = determineFlakyStatus(testCase, project, execution.getVersion(), item.status());

            testResultRepository.save(new TestResult(
                    item.status(),
                    isFlaky,
                    extraction.cleanError(),
                    extraction.screenshot(),
                    execution,
                    testCase
            ));
        }
    }

    /**
     * Calcula se o teste atual é Flaky usando os (N-1) resultados anteriores.
     */
    @Operation(summary = "Determina o estado Flaky", description = "Busca N-1 resultados históricos da DB e cruza com o status atual para identificar instabilidade na janela.")
    private boolean determineFlakyStatus(TestCase testCase, Project project, Version currentVersion, TestStatus currentStatus) {
        int threshold = project.getFlakyThreshold();

        if (threshold <= 1) {
            return false;
        }

        int previousNeeded = threshold - 1;
        List<TestResult> previousWindow;

        if (currentVersion != null) {
            previousWindow = testResultRepository.findRecentResultsByTestCaseProjectAndVersion(
                    testCase.getId(),
                    project.getId(),
                    currentVersion.getId(),
                    PageRequest.of(0, previousNeeded, Sort.by(Sort.Direction.DESC, "testExecution.startTime"))
            ).getContent();
        } else {
            previousWindow = testResultRepository.findRecentResultsByTestCaseAndProject(
                    testCase.getId(),
                    project.getId(),
                    PageRequest.of(0, previousNeeded, Sort.by(Sort.Direction.DESC, "testExecution.startTime"))
            ).getContent();
        }

        // Se ainda não temos histórico suficiente (N-1), a janela de N ainda não foi preenchida
        if (previousWindow.size() < previousNeeded) {
            return false;
        }

        // Inicia a contagem somando o resultado atual em memória
        long passCount = currentStatus == TestStatus.PASS ? 1 : 0;
        long failCount = currentStatus == TestStatus.FAIL ? 1 : 0;

        passCount += previousWindow.stream().filter(r -> r.getResult() == TestStatus.PASS).count();
        failCount += previousWindow.stream().filter(r -> r.getResult() == TestStatus.FAIL).count();

        return passCount > 0 && failCount > 0;
    }

    private record ExtractionResult(String cleanError, String screenshot) {}

    /**
     * Extração de imagens
     * Procura automaticamente por padrões de Base64 (JPEG ou PNG) dentro do log de erro,
     * ignorando quebras de linha introduzidas pelo XML.
     */
    private ExtractionResult extractScreenshotAndCleanError(String rawError) {
        if (rawError == null || rawError.isBlank()) {
            return new ExtractionResult(null, null);
        }

        String cleanError = rawError;
        String screenshot = null;

        Matcher matcher = SCREENSHOT_PATTERN.matcher(cleanError);

        if (matcher.find()) {
            screenshot = matcher.group(0).replaceAll("[\\r\\n]", "");
            cleanError = cleanError.replace(matcher.group(0), "[EVIDENCIA_VISUAL_EXTRAIDA]");
        }

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