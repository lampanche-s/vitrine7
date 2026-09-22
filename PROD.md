# PROD — Deploy seguro em produção

## Objetivo


Quando o usuário enviar exatamente `prod`, executar o fluxo de produção completo e seguro do projeto atual.

Este workflow é global. Use o `AGENTS.md`, documentação e infraestrutura real do projeto para descobrir comandos, serviços, caminhos, artefatos e health checks. Não invente dados de produção.

## Pré-condição obrigatória

Antes de qualquer alteração na VPS/produção, executar integralmente o fluxo `loc`.

Nunca fazer deploy se:

- build local não estiver 100% aprovado;
- testes locais não estiverem 100% aprovados;
- runtime local não estiver validado;
- Git estiver em estado inconsistente;
- houver segredo publicável detectado;
- houver conflito relevante não resolvido.

## Autonomia

- Corrigir automaticamente problemas comuns e seguros.
- Resolver divergências Git simples automaticamente.
- Tentar recuperação automática de falhas simples.
- Priorizar segurança acima de downtime.

## Ações que sempre exigem aprovação

- Exclusão de arquivos importantes.
- Alteração destrutiva em banco.
- Alteração de portas da VPS.
- Alteração de bancos da VPS.
- Mudança estrutural ampla ou de alto risco.
- Qualquer ação não reversível com risco relevante de perda de dados.

## Fluxo

1. Executar `loc` completo.
2. Garantir que a branch atual:
   - esteja sincronizada com o remoto;
   - não tenha conflitos pendentes;
   - tenha o commit final enviado.
3. Gerar os artefatos de produção localmente.
   - Não fazer build na VPS.
   - Implantar exatamente o que foi validado localmente.
4. Antes de alterar produção, verificar:
   - conectividade SSH;
   - espaço em disco;
   - serviços atuais;
   - health atual;
   - dependências necessárias;
   - estado do banco/migrations;
   - agentes/serviços auxiliares quando aplicável.
5. Criar e validar backup obrigatório antes do deploy:
   - banco;
   - artefatos atuais;
   - metadados/hashes suficientes para rollback.
6. Se o projeto usar releases:
   - implantar em diretório versionado;
   - manter referência clara para a release ativa;
   - manter as 3 releases anteriores válidas, além da ativa quando necessário.
7. Aplicar migrations:
   - não destrutivas e compatíveis: podem ser aplicadas automaticamente;
   - destrutivas ou com risco de perda de dados: exigir aprovação.
8. Estratégia de deploy:
   - usar blue-green/rolling apenas se já existir e estiver validado;
   - caso contrário, parar temporariamente os serviços quando isso for mais seguro;
   - evitar substituir artefatos de forma parcial/inconsistente.
9. Subir a nova versão.
10. Reiniciar/iniciar os serviços necessários.
11. Validar produção:
    - backend;
    - frontend;
    - banco;
    - migrations;
    - agentes;
    - logs;
    - health checks;
    - endpoints principais.
12. Executar smoke tests.
13. Se houver falha:
    - tentar correção simples e segura;
    - se não resolver, fazer rollback automático dos artefatos;
    - restaurar banco somente quando houver evidência clara de problema envolvendo banco/dados;
    - se rollback falhar, interromper e informar imediatamente.
14. Após sucesso:
    - registrar histórico do deploy com data, commit, resultado, backup e eventual rollback;
    - limpar releases antigas além da política definida, somente quando a exclusão for segura e autorizada pelo contrato do projeto;
    - garantir que produção permaneça rodando e saudável.

## Rollback padrão

- Priorizar rollback de artefatos.
- Banco não deve ser restaurado automaticamente por padrão.
- Antes de um rollback destrutivo, validar compatibilidade de schema/migrations.
- Se existir mecanismo de releases, preferir troca segura para a release anterior.

## Critérios de sucesso

O `prod` só termina quando:

- `loc` estiver 100% aprovado;
- Git estiver sincronizado;
- backup estiver confirmado;
- artefatos implantados forem os validados localmente;
- migrations estiverem corretas;
- backend/frontend/serviços estiverem saudáveis;
- agentes necessários estiverem validados;
- smoke tests passarem;
- produção permanecer rodando;
- histórico do deploy estiver registrado.

## Resumo final

Responder de forma curta contendo:

- commit/release implantado;
- backup criado;
- migrations aplicadas;
- serviços validados;
- agentes validados;
- smoke tests;
- rollback, se ocorreu;
- status final de produção.
