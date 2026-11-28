#!/usr/bin/env bash
# SSP1-155: checks that the pipeline runs on main and develop and enforces backend coverage.
# Lines are matched with their indentation, because a YAML key at the wrong depth changes its meaning.
set -u
cd "$(dirname "$0")/../.."
workflow=.github/workflows/maven.yml
pom=backend/pom.xml
fail=0

check() {
  if eval "$2"; then
    echo "ok   - $1"
  else
    echo "FAIL - $1"
    fail=1
  fi
}

has_line() {
  grep -qxF -- "$1" "$workflow"
}

check "the workflow has no tab characters" "! grep -q \$'\\t' $workflow"
check "on: is a top-level key with push and pull_request under it" \
  "has_line 'on:' && has_line '  push:' && has_line '  pull_request:'"
check "push and pull_request run on main and develop" \
  "[ \"\$(grep -cxF '    branches: [ \"main\", \"develop\" ]' $workflow)\" -eq 2 ]"
check "jobs: is a top-level key with the build job" "has_line 'jobs:' && has_line '  build:'"
check "the build step runs mvn verify with the formatter skipped" \
  "has_line '    - name: Build, test and check coverage' && has_line '      run: mvn -B verify -Dformatter.skip=true --file backend/pom.xml'"
check "the JaCoCo report is uploaded" \
  "has_line '      uses: actions/upload-artifact@v4' && has_line '        name: jacoco-report' && has_line '        path: backend/target/site/jacoco/'"
check "the pom defines coverage.line.minimum = 0.75" \
  "grep -q '<coverage.line.minimum>0.75</coverage.line.minimum>' $pom"
check "the pom runs the JaCoCo check goal" "grep -q '<goal>check</goal>' $pom"

if [ "$fail" -eq 0 ] && [ "${SKIP_GATE_RUN:-0}" != "1" ]; then
  echo "running verify with an impossible 0.99 minimum; it must fail on coverage"
  out=$(cd backend && ./mvnw -q verify -Dformatter.skip=true -Dcoverage.line.minimum=0.99 2>&1)
  if [ $? -ne 0 ] && echo "$out" | grep -q "Coverage checks have not been met"; then
    echo "ok   - the coverage gate fails the build below the minimum"
  else
    echo "FAIL - the coverage gate did not fail the build below the minimum"
    fail=1
  fi
fi
exit $fail
