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
```

Defina `REPORT_PROTECTED_PASSWORD` somente no arquivo protegido
`/etc/vitrine7/application.env`; nunca registre o valor real neste documento,
no Git, no frontend ou em logs. Mantenha também as variáveis de criptografia e
do perfil de pagamento já usadas no ambiente atual.

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
Flyway antes/depois e só conclui ao emitir `DEPLOY_OK`.

## Validar

```bash
ssh vitrine7-prod 'sudo -n /usr/local/sbin/vitrine7-deploy status'
```

Depois valide no navegador: login, Cadastro, Clientes, Comandas, quatro pagamentos, marcação de estorno, Histórico, comprovante, relatórios e download do backup `.backup`.

O backup baixado pela tela de Relatórios contém o banco completo, incluindo usuários e dados operacionais. Armazene-o fora da VPS e restrinja o acesso ao arquivo.

## Rollback

O wrapper atual restaura automaticamente o JAR anterior quando o novo backend
não fica saudável e o Flyway não avançou; também restaura o frontend anterior
quando sua validação falha. Se o Flyway avançar, ele interrompe sem executar
rollback de banco. Não existe subcomando manual autorizado de rollback.

Nunca restaure o banco ou altere `flyway_schema_history` manualmente. Rollback
de banco exige necessidade comprovada e aprovação explícita.

## Agente de impressão térmica

O backend precisa de um segredo exclusivo para autenticar o agente local de impressão:

```env
APP_PRINTER_AGENT_TOKEN=<token-forte-compartilhado-com-o-agente>
```

O agente Windows é distribuído separadamente em `vitrine7-printer-agent.zip` e usa o mesmo token em `printer-agent.properties`.

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
