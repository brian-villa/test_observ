# Flakiness Analysis Engine

SGMTA uses a **Sliding Window** algorithm to determine if a test is unstable or if its failure is consistent.

## How it works

Instead of looking at the entire history, the system focuses on the last $N$ executions (defined by the project's `flakyThreshold` attribute).

### The Three States of a Test:

1. **Stable (PASS):** The window contains *N* `PASS` results. The test is reliable.
2. **Stable (FAIL):** The window contains *N* `FAIL` results. The test is not unstable; it is simply broken and needs immediate fixing.
3. **Unstable (Flaky):** The window contains an alternation between `PASS` and `FAIL` results. The system marks the result as unstable and penalizes the Application Health (Health Score).

## Instability Recovery:

A test becomes stable and ceases to be Flaky as soon as it achieves **regularity**. If an unstable test passes $N$ consecutive times, the window becomes clean again, the Flaky status is removed, and the Health Score is restored.

## Why not change the history?

We believe in the immutability of facts. If Build #X was unstable two days ago, it will remain marked as unstable forever. Regularity only affects the **Current State** of the automation and future builds, ensuring that the history of a Test Case can be accurately tracked.