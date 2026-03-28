package com.example.sgmta.adapters;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;

/**
 * Contrato para todos os tipos de relatórios de CI/CD.
 * Qualquer formato novo deve implementar esta interface.
 */
public interface ReportAdapter {

    /**
     * O IngestionController vai usar este método para perguntar
     * a cada adaptador se ele sabe lidar com o ficheiro que acabou de chegar.
     *
     * @param contentType O cabeçalho HTTP (ex: "application/xml" ou "application/json")
     * @return true se o adaptador for o tradutor correto para este ficheiro.
     */
    boolean supports(String contentType);

    /**
     * @param rawPayload O corpo inteiro do pedido enviado pelo pipeline.
     * @param projectToken O token extraído do cabeçalho.
     * @param versionName A versão extraída do cabeçalho HTTP.
     * @param branchName A branch extraída do cabeçalho HTTP.
     */
    StandardizedPipelineReport adapt(String rawPayload, String projectToken, String versionName, String branchName);
}