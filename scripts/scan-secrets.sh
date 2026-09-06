#!/usr/bin/env bash
set -Eeuo pipefail

readonly GITLEAKS_IMAGE="${GITLEAKS_IMAGE:-zricethezav/gitleaks:latest}"
readonly MODE="${1:-publishable}"

repo_root="$(git rev-parse --show-toplevel)"
config_path="$repo_root/.gitleaks.toml"

[[ -f "$config_path" ]] || {
    printf 'Configuracao Gitleaks ausente: %s\n' "$config_path" >&2
    exit 2
}

run_tree_scan() {
    local tree_path="$1"

    docker run --rm \
        -v "$tree_path:/scan:ro" \
        -v "$config_path:/config/.gitleaks.toml:ro" \
        "$GITLEAKS_IMAGE" dir /scan \
        --config /config/.gitleaks.toml \
        --redact=100 \
        --no-banner
}

scan_publishable() (
    local temp_dir manifest tree
    temp_dir="$(mktemp -d)"
    manifest="$temp_dir/manifest"
    tree="$temp_dir/tree"
    trap 'rm -rf -- "$temp_dir"' EXIT

    mkdir -p "$tree"
    while IFS= read -r -d '' path; do
        if [[ -f "$repo_root/$path" || -L "$repo_root/$path" ]]; then
            printf '%s\0' "$path" >> "$manifest"
        fi
    done < <(git -C "$repo_root" ls-files --cached --others --exclude-standard -z)

    if [[ -s "$manifest" ]]; then
        tar --create --file=- --directory="$repo_root" --no-recursion \
            --null --files-from="$manifest" \
            | tar --extract --file=- --directory="$tree"
    fi

    run_tree_scan "$tree"
)

scan_staged() (
    local temp_dir tree
    temp_dir="$(mktemp -d)"
    tree="$temp_dir/tree"
    trap 'rm -rf -- "$temp_dir"' EXIT

    mkdir -p "$tree"
    git -C "$repo_root" checkout-index --all --prefix="$tree/"
    run_tree_scan "$tree"
)

scan_history() {
    docker run --rm \
        -v "$repo_root:/repo:ro" \
        -v "$config_path:/config/.gitleaks.toml:ro" \
        "$GITLEAKS_IMAGE" git /repo \
        --config /config/.gitleaks.toml \
        --redact=100 \
        --no-banner
}

scan_workspace_diagnostic() {
    local scanner_status

    set +e
    docker run --rm \
        -v "$repo_root:/workspace:ro" \
        -v "$config_path:/config/.gitleaks.toml:ro" \
        "$GITLEAKS_IMAGE" dir /workspace \
        --config /config/.gitleaks.toml \
        --redact=100 \
        --no-banner \
        --verbose
    scanner_status=$?
    set -e

    case "$scanner_status" in
        0)
            printf 'Diagnostico do workspace concluido sem achados.\n'
            ;;
        1)
            printf 'O workspace completo contem segredo(s); confirmando a superficie publicavel.\n' >&2
            if ! scan_publishable; then
                printf 'BLOQUEIO: pelo menos um achado pertence ao conteudo publicavel.\n' >&2
                return 1
            fi
            printf 'AVISO: os achados adicionais estao exclusivamente em arquivos locais ignorados e nao bloqueiam o fluxo Git.\n' >&2
            ;;
        *)
            printf 'Falha ao executar o diagnostico do workspace (codigo %s).\n' "$scanner_status" >&2
            return "$scanner_status"
            ;;
    esac
}

case "$MODE" in
    publishable)
        scan_publishable
        ;;
    staged)
        scan_staged
        ;;
    history)
        scan_history
        ;;
    workspace)
        scan_workspace_diagnostic
        ;;
    *)
        printf 'Uso: %s {publishable|staged|history|workspace}\n' "${0##*/}" >&2
        exit 2
        ;;
esac
