#!/bin/sh
# One-time developer setup. Run from the repo root after cloning.
set -e

cd "$(git rev-parse --show-toplevel)"

git config core.hooksPath .githooks
git config commit.template .gitmessage
chmod +x .githooks/* 2>/dev/null || true

echo "Git hooks path  -> $(git config core.hooksPath)"
echo "Commit template -> $(git config commit.template)"

key=$(git config jira.projectKey || true)
if [ -n "$key" ]; then
  echo "Jira project key -> $key"
else
  echo
  echo "Jira project key is not set. Once you know it, run:"
  echo "    git config jira.projectKey YOURKEY"
fi
