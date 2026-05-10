#!/bin/zsh
set -euo pipefail

cd "$(dirname "$0")"

mvn -q compile
exec mvn -q exec:java -Dexec.mainClass=client.ClientMain
