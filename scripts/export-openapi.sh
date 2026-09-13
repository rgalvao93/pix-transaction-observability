#!/usr/bin/env bash
# Exporta o contrato OpenAPI dos dois serviços (rodando) para api-spec/.
#
# Pré-requisito: os dois serviços no ar (transaction-service :8080, external-partner-mock :8081).
# Ex.: JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw spring-boot:run em cada módulo.
#
# O contrato canônico é o JSON em /v3/api-docs; este script o converte para YAML.
# Determinístico: o drift check regenera pelos mesmos passos e compara byte a byte.
#
# Cada endpoint é consultado com retry: durante o boot o Tomcat pode aceitar a
# conexão antes de o SpringDoc estar pronto, devolvendo corpo vazio/parcial.
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p api-spec

json2yaml() {
  python3 -c '
import json, sys, yaml
data = json.load(sys.stdin)
yaml.safe_dump(data, sys.stdout, sort_keys=False, allow_unicode=True,
               default_flow_style=False, width=1000)
'
}

# Faz GET /v3/api-docs em :$1 com até $2 tentativas, esperando JSON válido com
# a chave "openapi". Imprime o corpo na stdout e falha com diagnóstico caso o
# serviço não estabilize.
fetch_ready_spec() {
  local port="$1"
  local retries="${2:-20}"
  local tmp
  tmp="$(mktemp)"
  trap 'rm -f "$tmp"' RETURN

  for _ in $(seq 1 "$retries"); do
    if curl -fsSL "http://localhost:${port}/v3/api-docs" -o "$tmp" 2>/dev/null \
        && grep -q '"openapi"' "$tmp"; then
      cat "$tmp"
      return 0
    fi
    sleep 2
  done

  echo "ERRO: /v3/api-docs em :${port} não ficou pronto depois de ${retries} tentativas." >&2
  echo "Última resposta recebida (primeiros bytes):" >&2
  head -c 300 "$tmp" >&2
  echo >&2
  curl -sS -o /dev/null -w "status atual: http=%{http_code} bytes=%{size_download}\n" \
    "http://localhost:${port}/v3/api-docs" >&2 || true
  return 1
}

fetch_ready_spec 8080 | json2yaml > api-spec/transaction-service.openapi.yaml
fetch_ready_spec 8081 | json2yaml > api-spec/external-partner-mock.openapi.yaml

echo "Especificações exportadas em api-spec/:"
ls -l api-spec/*.yaml