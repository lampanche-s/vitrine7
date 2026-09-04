# Plano de endurecimento do deploy de produção

Inspeção somente leitura realizada em 2026-09-04. O wrapper ativo
`/usr/local/sbin/vitrine7-deploy` possui apenas `status`, `preflight` e `deploy`.
Ele já cria backup validado de banco/backend/frontend, restaura o JAR anterior
quando o health falha sem avanço do Flyway e restaura o frontend quando sua
validação falha.

As capacidades abaixo não existem hoje e não devem ser chamadas pelo Codex até
serem implementadas, revisadas e instaladas na VPS com aprovação explícita.

## Rollback explícito de artefatos

Adicionar ao wrapper um subcomando restrito a um diretório validado sob
`/opt/vitrine7/backups`. Ele deve:

1. validar nome, caminho real, ausência de symlinks, dump, JAR, frontend e hashes;
2. comparar Flyway atual com `flyway-before.txt`;
3. recusar rollback automático se as versões divergirem;
4. criar um novo backup antes da restauração;
5. restaurar somente backend/frontend, reiniciar apenas
   `vitrine7-backend.service` e exigir health/portas/site saudáveis;
6. nunca restaurar banco sem aprovação explícita e procedimento separado.

## Retenção das três últimas releases

Adicionar primeiro um modo somente leitura que liste candidatas por data e
preserve a release ativa e as três últimas. A exclusão deve ser um segundo modo
explícito, limitado a `/home/vitrine7-deploy/releases`, sem seguir symlinks e
somente após aprovação. Backups de banco terão política separada; não devem ser
apagados implicitamente junto com releases.

## Saúde dos agentes

O PagBank Agent já envia heartbeat persistido pelo backend. O Printer Agent só
faz long polling e ainda não possui heartbeat persistido. Para uma validação de
produção confiável será necessário:

1. persistir `lastSeenAt`, versão e identidade do Printer Agent;
2. criar uma consulta administrativa autenticada que agregue Printer/PagBank;
3. definir limites de offline sem tornar o health geral do backend dependente
   de uma estação que pode estar legitimamente desligada;
4. incluir essa consulta no `status` do wrapper sem incorporar credenciais ao
   script ou aos artefatos.

Isso envolve backend, banco, agentes Windows e instalação do wrapper na VPS;
portanto requer aprovação como mudança estrutural antes da implementação.
