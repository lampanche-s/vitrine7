#!/usr/bin/env bash
set -Eeuo pipefail

agent_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
jar_path="$agent_dir/target/vitrine7-printer-agent.jar"
config_path="${1:-$agent_dir/printer-agent.properties}"

[[ -f "$jar_path" ]] || {
    printf 'JAR ausente: %s. Execute: cd printer-agent && mvn clean verify\n' "$jar_path" >&2
    exit 1
}

[[ -f "$config_path" ]] || {
    printf 'Configuracao ausente: %s\n' "$config_path" >&2
    exit 1
}

exec java -jar "$jar_path" "$config_path"
