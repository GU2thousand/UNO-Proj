#!/bin/zsh
set -euo pipefail

PORT="${1:-5050}"
cd "$(dirname "$0")"

mvn -q compile
exec mvn -q exec:java -Dexec.mainClass=server.ServerMain -Dexec.args="$PORT"
