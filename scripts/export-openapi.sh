#!/usr/bin/env bash
# Exporta o contrato OpenAPI dos dois serviços (rodando) para api-spec/.
#
# Pré-requisito: os dois serviços no ar (transaction-service :8080, external-partner-mock :8081).
# Ex.: JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw spring-boot:run em cada módulo.
#
# O contrato canônico é o JSON em /v3/api-docs; este script o converte para YAML.
# Determinístico: o drift check regenera pelos mesmos passos e compara byte a byte.
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p api-spec

json2yaml() {
  python3 - <<'PY'
import json, sys, yaml
data = json.load(sys.stdin)
yaml.safe_dump(data, sys.stdout, sort_keys=False, allow_unicode=True,
               default_flow_style=False, width=1000)
PY
}

curl -fsSL http://localhost:8080/v3/api-docs | json2yaml > api-spec/transaction-service.openapi.yaml
curl -fsSL http://localhost:8081/v3/api-docs | json2yaml > api-spec/external-partner-mock.openapi.yaml

echo "Especificações exportadas em api-spec/:"
ls -l api-spec/*.yaml