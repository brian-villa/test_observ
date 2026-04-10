package com.example.sgmta.adapters;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JUnitXmlAdapter implements ReportAdapter {

    private final XmlMapper xmlMapper;

    public JUnitXmlAdapter() {
        this.xmlMapper = XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("application/xml");
    }

    @Override
    public StandardizedPipelineReport adapt(String rawPayload, String projectToken, String versionName, String branchName) {
        try {
            JUnitTestSuite suite = xmlMapper.readValue(rawPayload, JUnitTestSuite.class);

            List<StandardizedPipelineReport.TestCaseResult> standardizedTests = new ArrayList<>();

            if (suite.testCases != null) {
                for (JUnitTestCase testCase : suite.testCases) {
                    String status = determineStatus(testCase);
                    long durationMs = (long) (testCase.time * 1000);

                    String fullName = testCase.classname + "." + testCase.name;

                    standardizedTests.add(new StandardizedPipelineReport.TestCaseResult(
                            fullName,
                            status,
                            durationMs
                    ));
                }
            }

            LocalDateTime startTime = (suite.timestamp != null)
                    ? LocalDateTime.parse(suite.timestamp)
                    : LocalDateTime.now();

            LocalDateTime endTime = startTime.plusSeconds((long) suite.time);

            System.out.println(">>> suite.timestamp=" + suite.timestamp
                    + " suite.time=" + suite.time);

            return new StandardizedPipelineReport(
                    projectToken,
                    versionName,
                    branchName,
                    startTime,
                    endTime,
                    standardizedTests
            );

        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o formato JUnit XML: " + e.getMessage(), e);
        }
    }

    /**
     * Regra de negócio para traduzir a estrutura do JUnit para PASS/FAIL.
     */
    private String determineStatus(JUnitTestCase testCase) {
        if (testCase.failure != null) return "FAIL";
        if (testCase.skipped != null) return "SKIPPED";
        return "PASS";
    }

    @Schema(description = "Representa uma suite de testes dentro do relatório JUnit")
    public static class JUnitTestSuite {
        @JacksonXmlProperty(isAttribute = true)
        public String name;

        @JacksonXmlProperty(isAttribute = true)
        public String timestamp;

        @JacksonXmlProperty(isAttribute = true)
        public double time;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "testcase")
        public List<JUnitTestCase> testCases;
    }

    @Schema(description = "Representa o resultado individual de um teste no formato JUnit")
    public static class JUnitTestCase {
        @JacksonXmlProperty(isAttribute = true)
        public String name;

        @JacksonXmlProperty(isAttribute = true)
        public String classname;

        @JacksonXmlProperty(isAttribute = true)
        public double time;

        @JacksonXmlProperty(localName = "failure")
        public Object failure;

        @JacksonXmlProperty(localName = "error")
        public Object error;

        @JacksonXmlProperty(localName = "skipped")
        public Object skipped;
    }
}