package com.example.sgmta.integration.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Classe base para todos os testes de integração.
 * Usa uma base de dados H2 em memória (sem necessidade de Docker).
 * O contexto é recriado entre classes de teste para garantir isolamento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {
    // Métodos utilitários partilhados podem ser adicionados aqui.
}
