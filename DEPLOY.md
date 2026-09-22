# Deploy final do Vitrine 7

## Pré-requisitos

- Java 17
- PostgreSQL 16
- cliente PostgreSQL com `pg_dump` disponível para o usuário `vitrine7`
- Nginx
- serviço `vitrine7-backend.service`
- diretórios `/opt/vitrine7/backend` e `/opt/vitrine7/frontend`
- arquivo de ambiente `/etc/vitrine7/application.env`

## Variáveis mínimas

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://127.0.0.1:5432/vitrine7_db
DB_USERNAME=vitrine7
DB_PASSWORD=CONFIGURAR_FORA_DO_REPOSITORIO
JWT_SECRET=CONFIGURAR_FORA_DO_REPOSITORIO
CORS_ALLOWED_ORIGINS=https://vitrine7sys.duckdns.org
APP_ESTABLISHMENT_NAME=Vitrine 7
APP_ESTABLISHMENT_DOCUMENT=
APP_ESTABLISHMENT_PHONE=
APP_ESTABLISHMENT_ADDRESS=Rua Senhor do Bonfim, Monte Gordo, Camaçari/BA
APP_BACKUP_PG_DUMP_PATH=/usr/bin/pg_dump
APP_BACKUP_TIMEOUT=5m
REPORT_PROTECTED_PASSWORD=CONFIGURAR_FORA_DO_REPOSITORIO
CATALOG_PROTECTED_PASSWORD=CONFIGURAR_FORA_DO_REPOSITORIO
APP_PRINTER_AGENT_TOKEN=CONFIGURAR_FORA_DO_REPOSITORIO
APP_PRINTER_AGENT_OFFLINE_AFTER=90s
APP_PAYMENT_TERMINAL_BRIDGE_OFFLINE_AFTER=90s
```

Defina `REPORT_PROTECTED_PASSWORD` somente no arquivo protegido
`/etc/vitrine7/application.env`; nunca registre o valor real neste documento,
no Git, no frontend ou em logs. Mantenha também as variáveis de criptografia e
do perfil de pagamento já usadas no ambiente atual.

O exemplo `ops/application.env.production.example` contém somente nomes e
placeholders. Nunca registre valores reais nesse arquivo.

## Bootstrap do wrapper endurecido

O código instalável está versionado em `ops/vitrine7-deploy`. A instalação em
`/usr/local/sbin/vitrine7-deploy`, a criação de `/var/lib/vitrine7-deploy` e
qualquer ajuste de sudoers formam uma etapa separada, revisada e explicitamente
aprovada. A presença da fonte no Git não executa nem autoriza esse bootstrap.

Dependências: Bash, `flock`, `realpath`, `find`, GNU `tar`, `sha256sum`,
`unzip`, `psql`, `pg_dump`, `pg_restore`, `curl`, `ss`, `systemctl`, `timeout`,
`openssl` e `awk`. O arquivo de ambiente, as raízes de release/backup e todos os
componentes dos caminhos devem ser reais, nunca symlinks.

Após o bootstrap, os comandos previstos são:

```bash
sudo -n /usr/local/sbin/vitrine7-deploy status
sudo -n /usr/local/sbin/vitrine7-deploy preflight <release>
sudo -n /usr/local/sbin/vitrine7-deploy deploy <release>
sudo -n /usr/local/sbin/vitrine7-deploy agents-status --strict
sudo -n /usr/local/sbin/vitrine7-deploy releases
sudo -n /usr/local/sbin/vitrine7-deploy adopt-current-release <release>
```

`rollback-artifacts`, `rotate-report-credential` e
`releases --prune --confirm=DELETE-OLD-RELEASES` são mutáveis e exigem aprovação
operacional específica. A rotação não imprime a nova credencial e restaura o
arquivo anterior se o backend ou a autenticação operacional falharem.

## Gerar artefatos no Windows

Na raiz do projeto:

```powershell
.\scripts\cleanup-repository.ps1
.\scripts\build-release.ps1
```

O pacote final será criado em:

```text
release/vitrine7-release.zip
```

## Publicar pela VPS

O procedimento autorizado recebe uma pasta versionada em
`/home/vitrine7-deploy/releases/<release>` contendo exatamente
`vitrine7-backend.jar` e `frontend.tar.gz`. Depois de conferir os hashes locais
e remotos, use somente o wrapper:

```bash
ssh vitrine7-prod 'sudo -n /usr/local/sbin/vitrine7-deploy preflight <release>'
ssh vitrine7-prod 'sudo -n /usr/local/sbin/vitrine7-deploy deploy <release>'
```

O wrapper valida os artefatos, verifica o serviço e as portas protegidas, cria
backup completo do banco/backend/frontend em `/opt/vitrine7/backups`, registra
manifesto e hashes, registra Flyway antes/depois e só conclui ao emitir
`DEPLOY_OK`. O ledger protegido fica em
`/var/lib/vitrine7-deploy/deployments.jsonl` e não contém segredos.

## Validar

```bash
ssh vitrine7-prod 'sudo -n /usr/local/sbin/vitrine7-deploy status'
```

Depois da V53 e do bootstrap, `status` inclui o health agregado. Para validação
bloqueante use `agents-status --strict`: Printer e PagBank devem estar
`ONLINE`. `OFFLINE`, `NOT_CONFIGURED` e `REVOKED` não derrubam o health geral do
backend.

Depois valide no navegador: login, Cadastro, Clientes, Comandas, quatro pagamentos, marcação de estorno, Histórico, comprovante, relatórios e download do backup `.backup`.

O backup baixado pela tela de Relatórios contém o banco completo, incluindo usuários e dados operacionais. Armazene-o fora da VPS e restrinja o acesso ao arquivo.

## Rollback

O wrapper restaura automaticamente o JAR anterior quando o novo backend
não fica saudável e o Flyway não avançou; também restaura o frontend anterior
quando sua validação falha. Se o Flyway avançar, ele interrompe sem executar
rollback de banco.

`rollback-artifacts <backup>` valida caminho real, ausência de symlinks,
manifesto, dump e hashes; exige Flyway atual igual a `flyway-before.txt`; cria
novo backup completo; e restaura somente JAR/frontend. Nunca restaura banco.

Nunca restaure o banco ou altere `flyway_schema_history` manualmente. Rollback
de banco exige necessidade comprovada e aprovação explícita.

## Agente de impressão térmica

O backend precisa de um segredo exclusivo para autenticar o agente local de impressão:

```env
APP_PRINTER_AGENT_TOKEN=<token-forte-compartilhado-com-o-agente>
```

O agente Windows é distribuído separadamente em `vitrine7-printer-agent.zip` e usa o mesmo token em `printer-agent.properties`.

A V53 persiste identidade, versão e heartbeat. As propriedades
`agent.identity`, `agent.version` e `heartbeat.seconds` têm defaults
compatíveis; versões antigas continuam aceitas, e o long polling registra uma
identidade legada.

No Linux, execute:

```bash
cd printer-agent
mvn clean verify
./run-printer-agent.sh
```

## PagBank Agent

No Linux, copie `pagbank-agent/pagbank-agent.env.example` para o arquivo local
ignorado `pagbank-agent/pagbank-agent.env`, configure o pareamento sem versionar
segredos e execute:

```bash
cd pagbank-agent
mvn clean verify
./run-pagbank-agent.sh
```

## V44 - Fechamento de caixa e nota pre-pagamento

A V44 cria o registro simples de fechamento diario por usuario e separa os trabalhos de impressao entre comprovante final e nota de conferencia antes do pagamento. O mesmo Printer Agent continua sendo usado; nao ha novo processo local.

## Retenção e adoção inicial

`releases` apenas lista `ACTIVE`, `KEEP_LAST_3` e `CANDIDATE`. A exclusão é um
modo separado e confirmado e nunca remove backups. Na primeira instalação, use
`adopt-current-release <release>` somente se os hashes do JAR e de toda a árvore
do frontend coincidirem com os artefatos ativos.
