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
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/vitrine7_db
SPRING_DATASOURCE_USERNAME=vitrine7
SPRING_DATASOURCE_PASSWORD=ALTERAR
APP_SECURITY_JWT_SECRET=ALTERAR_COM_SEGREDO_FORTE
APP_ESTABLISHMENT_NAME=Vitrine 7
APP_ESTABLISHMENT_DOCUMENT=
APP_ESTABLISHMENT_PHONE=
APP_ESTABLISHMENT_ADDRESS=Rua Senhor do Bonfim, Monte Gordo, Camaçari/BA
APP_BACKUP_PG_DUMP_PATH=/usr/bin/pg_dump
APP_BACKUP_TIMEOUT=5m
```

Mantenha também as variáveis de criptografia e do perfil de pagamento já usadas no ambiente atual.

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

## Backup na VPS

```bash
sudo systemctl stop vitrine7-backend.service
sudo -u postgres pg_dump -Fc vitrine7_db > /opt/vitrine7/backup-pre-v41.dump
sudo cp /opt/vitrine7/backend/vitrine7-backend.jar /opt/vitrine7/backend/vitrine7-backend.jar.rollback
sudo cp -a /opt/vitrine7/frontend /opt/vitrine7/frontend.rollback
```

## Publicar

Copie o conteúdo do pacote para uma pasta temporária na VPS e execute:

```bash
sudo install -o vitrine7 -g vitrine7 -m 0640 backend/vitrine7-backend.jar /opt/vitrine7/backend/vitrine7-backend.jar
sudo rm -rf /opt/vitrine7/frontend/*
sudo cp -a frontend/. /opt/vitrine7/frontend/
sudo chown -R vitrine7:vitrine7 /opt/vitrine7/frontend
sudo systemctl start vitrine7-backend.service
sudo systemctl reload nginx
```

## Validar

```bash
sudo systemctl status vitrine7-backend.service --no-pager
sudo journalctl -u vitrine7-backend.service -n 120 --no-pager
curl -fsS http://127.0.0.1:8081/actuator/health
curl -I https://vitrine7sys.duckdns.org
```

Depois valide no navegador: login, Cadastro, Clientes, Comandas, quatro pagamentos, marcação de estorno, Histórico, comprovante, relatórios e download do backup `.backup`.

O backup baixado pela tela de Relatórios contém o banco completo, incluindo usuários e dados operacionais. Armazene-o fora da VPS e restrinja o acesso ao arquivo.

## Rollback

A V41 adiciona o estoque simplificado ao catálogo. Em caso de falha grave:

```bash
sudo systemctl stop vitrine7-backend.service
sudo cp /opt/vitrine7/backend/vitrine7-backend.jar.rollback /opt/vitrine7/backend/vitrine7-backend.jar
sudo rm -rf /opt/vitrine7/frontend/*
sudo cp -a /opt/vitrine7/frontend.rollback/. /opt/vitrine7/frontend/
sudo -u postgres dropdb --if-exists vitrine7_db
sudo -u postgres createdb vitrine7_db
sudo -u postgres pg_restore -d vitrine7_db /opt/vitrine7/backup-pre-v41.dump
sudo systemctl start vitrine7-backend.service
sudo systemctl reload nginx
```

Não remova manualmente linhas de `flyway_schema_history`.

## Agente de impressão térmica

O backend precisa de um segredo exclusivo para autenticar o agente local de impressão:

```env
APP_PRINTER_AGENT_TOKEN=<token-forte-compartilhado-com-o-agente>
```

O agente Windows é distribuído separadamente em `vitrine7-printer-agent.zip` e usa o mesmo token em `printer-agent.properties`.
