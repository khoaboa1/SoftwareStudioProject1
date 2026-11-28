#!/usr/bin/env bash
# SSP1-151: runs the backend tests with JaCoCo and refreshes the committed coverage evidence in docs/coverage/.
set -euo pipefail
cd "$(dirname "$0")/.."
(cd backend && ./mvnw -q verify -Dformatter.skip=true)
mkdir -p docs/coverage
cp backend/target/site/jacoco/jacoco.xml docs/coverage/jacoco.xml
python3 scripts/coverage_summary.py docs/coverage/jacoco.xml docs/coverage/README.md
echo "Wrote docs/coverage/README.md and docs/coverage/jacoco.xml"
