#!/usr/bin/env bash
# SSP1-154: checks the README's design patterns section: at least two patterns, each with
# where / why / code, every referenced repo path exists, and code indentation survived.
set -u
cd "$(dirname "$0")/.."
fail=0

check() {
  if eval "$2"; then
    echo "ok   - $1"
  else
    echo "FAIL - $1"
    fail=1
  fi
}

section=$(awk '/^## Design patterns$/{on=1; print; next} on && /^## /{exit} on{print}' README.md)

check "README has a '## Design patterns' section" "[ -n \"\$section\" ]"
check "at least two patterns are documented" "[ \"\$(printf '%s\n' \"\$section\" | grep -c '^### ')\" -ge 2 ]"
check "every pattern says where it is used" "[ \"\$(printf '%s\n' \"\$section\" | grep -c '^\*\*Where:\*\*')\" -eq \"\$(printf '%s\n' \"\$section\" | grep -c '^### ')\" ]"
check "every pattern says why it was chosen" "[ \"\$(printf '%s\n' \"\$section\" | grep -c '^\*\*Why:\*\*')\" -eq \"\$(printf '%s\n' \"\$section\" | grep -c '^### ')\" ]"
check "every pattern has a code example" "[ \"\$(printf '%s\n' \"\$section\" | grep -cE '^\`\`\`(java|ts)\$')\" -ge \"\$(printf '%s\n' \"\$section\" | grep -c '^### ')\" ]"
check "code blocks kept their indentation" "printf '%s\n' \"\$section\" | grep -qxF '    boolean send(String to, String pin);   // false = not configured or failed, try the next one'"

missing=""
for path in $(printf '%s\n' "$section" | grep -oE '`(backend|frontend|docs)/[^` ]+`' | tr -d '`' | sort -u); do
  [ -e "$path" ] || missing="$missing $path"
done
check "every referenced file exists${missing:+ (missing:$missing)}" "[ -z \"\$missing\" ]"

exit $fail
