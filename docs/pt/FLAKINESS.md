# Motor de Análise de Flakiness

O SGMTA utiliza um algoritmo de **Slide Window** para determinar se um teste é instável ou se a sua falha é consistente.

## Como funciona?

Em vez de olhar para o histórico total, o sistema foca-se nas últimas $N$ execuções (definidas pelo atributo `flakyThreshold` do projeto).

### Os Três Estados de um Teste:

1. **Estável (PASS):** A janela contém *N* resultados `PASS`. O teste é de confiança.
2. **Estável (FAIL):** A janela contém *N* resultados `FAIL`. O teste não é instável, está simplesmente partido e precisa de correção imediata.
3. **Instável (Flaky):** A janela contém alternância entre resultados `PASS` e `FAIL`. O sistema marca o resultado como instável e penaliza a Saúde da Aplicação.

## Recuperação da Instabilidade:

Um teste torna-se estável ao deixa de ser Flaky assim que atinge a **regularidade**. Se um teste instável passar $N$ vezes seguidas, a janela torna-se limpa novamente, o estado Flaky é removido e o Health Score é restaurado.

## Porque não alterar o histórico?

Acreditamos na imutabilidade dos factos. Se a Build #X foi instável há dois dias, ela ficará marcada como instável para sempre. A regularidade afeta apenas o **Estado Atual** da automação e as builds futuras. Garantindo que seja acompanhado o historial de um Test Case.


