# Backend test coverage

Generated from `docs/coverage/jacoco.xml` by `scripts/coverage-report.sh` (JaCoCo, `./mvnw verify`).

**PASS**: line coverage 92.4% meets the 70% rubric minimum.

| Counter | Covered / total | Coverage |
|---|---|---|
| Line | 1465 / 1585 | 92.4% |
| Branch | 464 / 602 | 77.1% |
| Instruction | 7569 / 8051 | 94.0% |
| Method | 473 / 512 | 92.4% |
| Class | 121 / 121 | 100.0% |

## By package

| Package | Lines | Line % | Branches | Branch % |
|---|---|---|---|---|
| com.Handoff.backend | 1 / 3 | 33.3% | 0 / 0 | n/a |
| com.Handoff.backend.config | 158 / 158 | 100.0% | 37 / 38 | 97.4% |
| com.Handoff.backend.controller | 202 / 211 | 95.7% | 14 / 18 | 77.8% |
| com.Handoff.backend.dto | 163 / 185 | 88.1% | 61 / 64 | 95.3% |
| com.Handoff.backend.model | 215 / 255 | 84.3% | 21 / 56 | 37.5% |
| com.Handoff.backend.repository | 25 / 25 | 100.0% | 8 / 8 | 100.0% |
| com.Handoff.backend.service | 500 / 517 | 96.7% | 237 / 294 | 80.6% |
| com.Handoff.backend.service.auth | 124 / 136 | 91.2% | 60 / 92 | 65.2% |
| com.Handoff.backend.service.email | 50 / 63 | 79.4% | 15 / 18 | 83.3% |
| com.Handoff.backend.service.storage | 27 / 32 | 84.4% | 11 / 14 | 78.6% |

The full HTML report is uploaded by CI as the `jacoco-report` artifact, or run `scripts/coverage-report.sh` and open `backend/target/site/jacoco/index.html`.
