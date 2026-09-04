#!/usr/bin/env bash
set -Eeuo pipefail

agent_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
jar_path="$agent_dir/target/vitrine7-pagbank-agent.jar"
env_path="${1:-$agent_dir/pagbank-agent.env}"

[[ -f "$jar_path" ]] || {
    printf 'JAR ausente: %s. Execute: cd pagbank-agent && mvn clean verify\n' "$jar_path" >&2
    exit 1
}

if [[ -f "$env_path" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$env_path"
    set +a
elif [[ "$#" -gt 0 ]]; then
    printf 'Configuracao ausente: %s\n' "$env_path" >&2
    exit 1
fi

cd "$agent_dir"
exec java -jar "$jar_path"
