# Instruções operacionais do workspace Vitrine 7

Este arquivo se aplica a todo o repositório. Quando o usuário enviar `loc` ou
`prod` como comando curto (isolado ou seguido de observações), trate-o como a
solicitação para executar integralmente o fluxo correspondente abaixo. Não é um
alias de shell: é uma instrução permanente para o Codex neste workspace.

## Princípios obrigatórios

- Trabalhe sempre a partir da raiz deste repositório e inspecione o estado real
  antes de agir. Não reutilize PID, hash, versão, contagem, release ou estado de
  serviço de uma execução anterior.
- Nunca exponha, copie para o repositório nem registre em logs credenciais,
  tokens, senhas, conteúdo de arquivos de ambiente ou material de chave SSH.
- Não invente comandos, portas, serviços, credenciais ou caminhos. Quando um
  requisito não tiver mecanismo real e verificável, pare nesse ponto como
  problema sério e informe exatamente o que falta.
- Antes de encerrar qualquer processo, use `ss -ltnp` e `ps -eo
  pid,ppid,user,lstart,args --sort=pid`; confirme pelo comando e pelo diretório
  de trabalho que ele pertence a este checkout. Envie primeiro `SIGTERM`,
  aguarde e confira novamente. Nunca mate um processo só porque ocupa `8080` ou
  `5173` e não toque em processos de outros projetos. No fluxo `loc`, encerre o
  PostgreSQL ao final somente se o próprio fluxo o tiver iniciado; se o serviço
  já estava ativo antes da execução, preserve-o.
- Preserve e inclua alterações já existentes no fluxo Git solicitado por
  `loc`/`prod`. Não descarte, reverta ou sobrescreva trabalho preexistente.
- Correções pequenas, previsíveis, reversíveis e claramente dentro do escopo
  podem ser feitas automaticamente e devem ser revalidadas. Interrompa em erro
  sério/crítico, dúvida sobre propriedade de processo/dado, falta de credencial,
  falha persistente, risco de perda ou ausência de comando autorizado.
- Peça aprovação antes de excluir arquivos importantes, executar mudança
  destrutiva de banco, alterar portas ou bancos da VPS e realizar mudança
  estrutural ampla ou de alto risco. Migration destrutiva e rollback de banco
  sempre exigem aprovação explícita.

## Mapa real do projeto

### Local

- Backend Spring Boot/Java 17: `backend/`; build e testes completos: `cd
  backend && mvn clean verify`; execução dev: `cd backend && mvn
  spring-boot:run`; porta padrão `8080`; health público da aplicação:
  `curl -fsS http://127.0.0.1:8080/api/v1/health`.
- Frontend React/Vite: `frontend/`; instalação reprodutível: `cd frontend &&
  npm ci`; build: `npm run build`; lint: `npm run lint`; testes: `npm test`;
  execução: `npm run dev`; porta padrão `5173`; `.env.local` aponta para
  `http://localhost:8080/api/v1`; validação básica: `curl -fsS
  http://127.0.0.1:5173/`.
- PostgreSQL local: serviço systemd `postgresql`, listener `127.0.0.1:5432`,
  banco `vitrine7_db`. Verificações: `systemctl is-active postgresql`,
  `pg_isready -h 127.0.0.1 -p 5432` e consulta somente leitura por `psql`.
  As credenciais exclusivas locais do Vitrine 7 ficam em `.codex-run/loc.env`,
  arquivo ignorado e com modo `0600`, contendo `DB_USERNAME` e `DB_PASSWORD`.
  Todo fluxo local deve validar sua existência e permissões e carregá-lo com
  `set -a; . .codex-run/loc.env; set +a` antes de consultas, Flyway, testes ou
  inicialização do backend; nunca imprima a senha na linha de comando ou saída.
- Flyway: migrations em `backend/src/main/resources/db/migration`; não há CLI
  ou plugin Maven Flyway separado. `application-dev.yml` habilita
  `validate-on-migrate`; portanto a forma real de validar/aplicar migrations é
  iniciar o backend com o profile `dev`. Confirme antes/depois por consulta
  somente leitura a `flyway_schema_history`. Nunca execute `flyway repair` às
  cegas nem altere essa tabela manualmente.
- Printer Agent: `printer-agent/`; build/testes: `cd printer-agent && mvn clean
  verify`; JAR: `printer-agent/target/vitrine7-printer-agent.jar`; launcher real
  no Linux: `./printer-agent/run-printer-agent.sh`; launcher do pacote Windows:
  `printer-agent/run-printer-agent.cmd`. Ambos executam o mesmo JAR e arquivo
  `printer-agent.properties`. O arquivo local é sensível e ignorado pelo Git. O
  log `Comunicacao com o backend validada.` comprova uma resposta autenticada do
  backend.
- PagBank Agent: `pagbank-agent/`; build/testes: `cd pagbank-agent && mvn clean
  verify`; JAR executável com dependências:
  `pagbank-agent/target/vitrine7-pagbank-agent.jar`; launcher Linux:
  `./pagbank-agent/run-pagbank-agent.sh`. A configuração local opcional é
  `pagbank-agent/pagbank-agent.env`, ignorada pelo Git e derivada de
  `pagbank-agent.env.example`. O log `Heartbeat enviado.` comprova comunicação
  autenticada. Este workspace possui token local pareado no diretório ignorado
  `pagbank-agent/.state`; valide sua existência sem ler ou imprimir o conteúdo.
  Nunca improvise credenciais de pareamento.
- Auditoria/release Windows: `powershell -File
  .\scripts\final-audit.ps1` e `powershell -File
  .\scripts\build-release.ps1`; este último roda os quatro módulos e gera
  `release/vitrine7-release.zip` e `release/vitrine7-printer-agent.zip`.
  `scripts/cleanup-repository.ps1` apaga artefatos gerados; não o execute sem
  confirmar o escopo e obter aprovação quando houver arquivos importantes.
  Nesta estação Linux, `pwsh`/`powershell` não estão instalados.
- Git: repositório na raiz, branch atual descoberta por `git branch
  --show-current`. Fluxo solicitado: `git status --short --branch`, `git diff`
  e `git diff --cached`, `git add -A`, commit com mensagem baseada no diff,
  `git pull --rebase` e `git push`. No estado inspecionado em 2026-09-04 a branch
  era `master`, com remoto privado `origin` e upstream `origin/master`; sempre
  verifique novamente e trate ausência ou divergência como bloqueio sério. Este
  checkout usa `core.whitespace=cr-at-eol` para aceitar os arquivos CRLF
  preexistentes sem ocultar outros erros de whitespace. A varredura bloqueante
  de segredos usa `./scripts/scan-secrets.sh publishable` para a árvore que pode
  ser versionada, `./scripts/scan-secrets.sh staged` para o índice exato e
  `./scripts/scan-secrets.sh history` para todo o histórico. Segredo encontrado
  em arquivo rastreado, staged, não ignorado/publicável ou no histórico bloqueia
  commit e push. `./scripts/scan-secrets.sh workspace` é um diagnóstico opcional
  do workspace completo: achado exclusivamente em arquivo local explicitamente
  ignorado pelo Git gera aviso, não bloqueio. Todos os modos usam redação total;
  nunca exiba o valor encontrado nem crie allowlist para segredo verdadeiro.

### Produção

- SSH: alias `vitrine7-prod`, usuário remoto `vitrine7-deploy`, diretório de
  releases `/home/vitrine7-deploy/releases/<release>`.
- Artefatos aceitos pelo deploy real: `vitrine7-backend.jar` e
  `frontend.tar.gz`. No Linux, após builds aprovados, prepare uma pasta local
  exclusiva `.codex-run/<release>`, copie
  `backend/target/vitrine7-backend-0.0.1-SNAPSHOT.jar` como
  `vitrine7-backend.jar`, e gere o frontend com `tar -C frontend/dist -czf
  .codex-run/<release>/frontend.tar.gz .`. Calcule ambos com `sha256sum`.
- Crie apenas `/home/vitrine7-deploy/releases/<release>` com `ssh`, envie os
  dois arquivos com `scp` e compare os SHA-256 local/remoto antes de prosseguir.
- Operação privilegiada autorizada: exclusivamente `ssh vitrine7-prod 'sudo
  -n /usr/local/sbin/vitrine7-deploy preflight <release>'`, depois `ssh
  vitrine7-prod 'sudo -n /usr/local/sbin/vitrine7-deploy deploy <release>'` e,
  somente após `DEPLOY_OK`, `ssh vitrine7-prod 'sudo -n
  /usr/local/sbin/vitrine7-deploy status'`. Não substitua o wrapper por
  `systemctl`, `psql`, `cp`, `mv`, `rm` ou comandos privilegiados manuais.
- Serviço conhecido: `vitrine7-backend.service`; backend servido internamente
  em `8081`; o status autorizado também protege/mostra listeners `8080` e
  `8081`. Health: `curl -fsS http://127.0.0.1:8081/actuator/health` na VPS;
  frontend público: `curl -fsS https://vitrine7sys.duckdns.org`.
- O wrapper de deploy cria backup versionado sob `/opt/vitrine7/backups` e
  informa Flyway antes/depois. `DEPLOY.md` contém um procedimento manual antigo;
  não o use enquanto o wrapper autorizado existir.
- A fonte endurecida está em `ops/vitrine7-deploy`, mas seus novos subcomandos
  só existem operacionalmente após bootstrap explícito na VPS. Até confirmar
  instalação e sudoers reais, não chame `rollback-artifacts`, `releases`,
  `adopt-current-release`, `rotate-report-credential` ou `agents-status`.
  Versionar a fonte não autoriza bootstrap nem rotação.
- Depois do bootstrap confirmado, `releases` é somente leitura por padrão e
  preserva a ativa mais as três releases não ativas mais recentes. O modo
  `--prune --confirm=DELETE-OLD-RELEASES`, `rollback-artifacts` e
  `rotate-report-credential` exigem aprovação explícita em cada uso. Backups
  nunca participam da retenção de releases.
- O wrapper restaura automaticamente o JAR anterior se o novo backend falhar
  sem avanço do Flyway, e restaura o frontend anterior se a validação pública
  falhar. Não existe subcomando explícito de rollback; se o Flyway avançar ele
  interrompe sem rollback de banco. Não invente rollback manual. Rollback de
  banco exige necessidade comprovada e aprovação explícita.
- Printer Agent e PagBank Agent são agentes da estação Windows, não serviços da
  VPS. A V53 adiciona heartbeat persistido do Printer e o endpoint operacional
  agrega ambos sem afetar o health geral. Depois do bootstrap confirmado, exija
  `agents-status --strict`; antes dele, a ausência do mecanismo é bloqueio sério.

## Comando `loc`

Execute nesta ordem, mantendo logs temporários fora dos arquivos versionados:

1. Capture branch, `git status`, diffs, processos/listeners e ferramentas. Não
   altere o Git ainda.
2. Encerre somente instâncias antigas comprovadamente pertencentes a este
   checkout. Se `8080`/`5173` pertencerem a outro projeto, não os encerre e pare
   como conflito sério; não escolha outra porta sem base real.
3. Verifique PostgreSQL. Se estiver inativo, inicie somente o serviço
   `postgresql` pelo mecanismo systemd disponível e confirme com `pg_isready`.
   Antes de qualquer consulta, Flyway, teste ou backend, confirme que
   `.codex-run/loc.env` existe, está ignorado pelo Git e tem modo `0600`; então
   carregue `DB_USERNAME` e `DB_PASSWORD` sem shell trace e sem imprimir seus
   valores. A ausência, permissão incorreta ou falha de autenticação é bloqueio
   sério; não use usuário/credencial de outro projeto.
4. Liste migrations locais, consulte `flyway_schema_history` e revise cada
   migration pendente. Mudança destrutiva exige aprovação. Para migrations
   seguras, carregue `APP_PRINTER_AGENT_TOKEN` a partir de `agent.token` do
   arquivo local ignorado sem ativar shell trace nem imprimir o valor; inicie
   temporariamente o backend dev, aguarde Flyway concluir, valide a versão e
   encerre essa instância antes da bateria completa.
5. Execute `mvn clean verify` em `backend`, `pagbank-agent` e `printer-agent`.
   No frontend execute `npm ci`, `npm run build`, `npm run lint` e `npm test`.
   Corrija automaticamente apenas falhas simples e seguras e repita a validação
   afetada; tudo deve ficar 100% aprovado.
6. Crie `.codex-run/agents`, inicie temporariamente backend, frontend,
   `./printer-agent/run-printer-agent.sh` e
   `./pagbank-agent/run-pagbank-agent.sh`, cada um em background com log e PID
   próprios. Use somente configurações locais ignoradas pelo Git e registre os
   PIDs exatos. Ausência de configuração/token/pareamento válido é bloqueio
   sério; nunca use os agentes locais contra produção durante `loc`.
7. Valide PostgreSQL, Flyway, `/api/v1/health`, frontend e o funcionamento real
   dos agentes: exija processo vivo e os marcadores de log `Comunicacao com o
   backend validada.` e `Heartbeat enviado.`. Faça smoke tests seguros dos fluxos
   disponíveis sem criar/corromper dados reais.
8. Somente após validação total, execute o Git completo na branch atual:
   rode `./scripts/scan-secrets.sh publishable` e
   `./scripts/scan-secrets.sh history` -> opcionalmente rode
   `./scripts/scan-secrets.sh workspace` apenas como diagnóstico ->
   `status/diff` -> `git add -A` (incluindo mudanças preexistentes) -> revise o
   staged diff e rode `./scripts/scan-secrets.sh staged` -> crie mensagem fiel
   ao conjunto -> commit -> `git pull --rebase` do upstream -> resolva apenas
   conflitos simples e inequívocos -> revalide e repita as varreduras
   bloqueantes se houve integração -> `git push`. Nunca faça commit/push com
   segredo no conteúdo publicável, staged ou histórico. Segredo exclusivamente
   em arquivo local explicitamente ignorado gera aviso e deve ser preservado;
   não o adicione, mova, copie, imprima ou use para novo pareamento. Ausência de
   scanner, remoto/upstream, conflito não trivial ou rejeição de push é problema
   sério.
9. Em bloco de limpeza garantido mesmo após falha, envie `SIGTERM` somente aos
   PIDs deste checkout iniciados/identificados pela execução e confirme que
   ficaram encerrados. Encerre PostgreSQL somente se ele tiver sido iniciado
   pelo próprio fluxo `loc`; se já estava ativo antes da execução, preserve o
   serviço. Mantenha processos alheios rodando.
10. Entregue resumo curto: validações, migrations, commit/pull/push, correções,
    serviços encerrados e qualquer bloqueio.

## Comando `prod`

1. Execute primeiro todo o fluxo `loc`. Não prossiga se qualquer build, teste,
   runtime, migration, agente ou sincronização Git não estiver 100% aprovado.
2. Confirme que `HEAD` está publicado no upstream e que o worktree ficou limpo
   após o Git do `loc`.
3. Gere novamente os artefatos localmente a partir desse `HEAD`; dê à release
   um nome seguro, identificável e versionado. Confirme JAR, `dist/index.html`,
   archive e hashes.
4. Antes de enviar, faça inspeção SSH somente leitura: identidade
   `vitrine7-deploy`, conectividade, espaço em disco, diretórios permitidos,
   estado do serviço, health, site público e status Flyway. Não prossiga com
   disco insuficiente, serviço já degradado sem causa entendida ou host errado.
5. Garanta backup obrigatório usando o wrapper autorizado, que cria o backup de
   banco e artefatos antes da troca. O deploy só é válido se a saída identificar
   o backup criado. Não faça backup manual privilegiado em paralelo.
6. Preserve releases versionadas e no mínimo as três últimas. Antes de remover
   uma release/backup mais antiga, verifique precisamente a política e peça
   aprovação por ser exclusão de artefato importante; nunca faça limpeza ampla.
7. Envie os artefatos, compare hashes e execute `preflight`. Só execute `deploy`
   se o preflight terminar com `PREFLIGHT_OK` e migrations pendentes forem
   classificadas como seguras. Migration destrutiva exige aprovação.
8. Aguarde o wrapper concluir. Exija `DEPLOY_OK`, caminho do backup e Flyway
   antes/depois. Depois execute `status` e, somente se o bootstrap endurecido
   estiver confirmado, `agents-status --strict`.
9. Valide backend, frontend, PostgreSQL/Flyway por evidência do wrapper, logs
   disponibilizados por ele, health interno, site público e smoke tests do
   `DEPLOY.md`. Testes autenticados e agentes exigem configuração/sessão válida;
   não fabrique credenciais. Confirme Printer/PagBank por heartbeat/dispositivo,
   ou declare bloqueio em vez de presumir saúde.
10. Corrija automaticamente somente falhas simples, seguras e permitidas pelo
    wrapper. Conte apenas com os rollbacks automáticos internos já descritos;
    não existe comando externo autorizado de rollback. Não faça rollback manual
    do banco sem necessidade comprovada e aprovação.
11. Registre o histórico pela release versionada, `HEAD`, hashes, saída do
    preflight/deploy/status, backup e Flyway antes/depois. Só considere concluído
    se produção permanecer ativa e saudável; então forneça resumo curto.
