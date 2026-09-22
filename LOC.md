# LOC — Validação e execução local

## Objetivo

Quando o usuário enviar exatamente `loc`, executar o fluxo local completo do projeto atual.

Este workflow é global. Descubra os comandos e detalhes específicos no `AGENTS.md`, documentação e estrutura do projeto. Não invente portas, credenciais, caminhos, serviços ou comandos.

## Autonomia

- Corrija automaticamente problemas comuns, previsíveis e seguros.
- Reexecute a etapa afetada após a correção.
- Interrompa apenas quando houver risco sério, crítico, destrutivo ou possibilidade relevante de sobrescrever trabalho importante.
- Evite refactors fora do escopo.
- Preserve decisões e arquitetura existentes.

## Ações que sempre exigem aprovação

- Exclusão de arquivos importantes.
- Alteração destrutiva em banco de dados.
- Alteração de portas de VPS.
- Alteração de bancos de VPS.
- Mudança estrutural ampla ou de alto risco.

## Fluxo

1. Inspecionar o estado atual do projeto e a branch atual.
2. Preservar alterações locais existentes e incluí-las no fluxo final de Git.
3. Identificar e encerrar apenas processos antigos pertencentes ao projeto.
   - Nunca encerrar processos de outro projeto só porque ocupam uma porta.
   - Se houver conflito com outro projeto, interromper e informar.
4. Verificar serviços locais necessários, como banco de dados.
   - Se o banco estiver parado e for seguro iniciá-lo, iniciar.
   - Se já estava ativo antes do `loc`, preservá-lo ao final.
   - Se o próprio `loc` iniciou o banco, encerrá-lo ao final.
5. Carregar configuração local segura necessária ao projeto.
   - Segredos locais devem ficar fora do Git e com permissões restritas quando aplicável.
6. Aplicar migrations locais pendentes quando forem compatíveis e seguras.
7. Executar validações completas do projeto:
   - instalação/restauração de dependências quando necessário;
   - build;
   - lint;
   - testes;
   - verificações adicionais definidas pelo projeto.
8. Se houver componentes auxiliares/agentes locais, validar build/testes deles também.
9. Subir temporariamente os serviços do projeto.
10. Validar runtime real:
    - backend;
    - frontend;
    - banco;
    - agentes;
    - health checks/endpoints;
    - portas e respostas esperadas.
11. Em falha:
    - ler logs;
    - corrigir automaticamente quando simples e seguro;
    - reiniciar/revalidar;
    - interromper apenas se o problema for sério, crítico ou persistente.
12. Executar secret scanning antes do Git:
    - bloquear se houver segredo no histórico Git, arquivos rastreados, staged ou conteúdo publicável;
    - arquivos locais explicitamente ignorados podem gerar aviso, mas não devem bloquear;
    - nunca imprimir o valor de um segredo.
13. Somente depois de toda a validação, executar Git na branch atual:
    - `status`;
    - `diff`;
    - `add`;
    - gerar mensagem de commit automaticamente com base no diff;
    - `commit`;
    - `pull --rebase` ou equivalente seguro;
    - resolver conflitos simples automaticamente;
    - interromper se houver risco de sobrescrever lógica importante;
    - `push`.
14. Não trocar automaticamente para `main`/`master`; trabalhar na branch atual.
15. Incluir no mesmo commit final:
    - alterações locais preexistentes;
    - mudanças feitas durante a tarefa;
    - correções automáticas pequenas e seguras.
16. Encerrar os processos temporários iniciados pelo `loc`.
17. Confirmar que o worktree terminou limpo e sincronizado, quando aplicável.

## Critérios de sucesso

O `loc` só pode ser considerado concluído quando:

- build(s) aprovados;
- testes aprovados;
- migrations locais válidas;
- runtime validado;
- serviços/agentes necessários validados;
- secret scan bloqueante aprovado;
- Git concluído e sincronizado;
- processos temporários encerrados;
- nenhum problema crítico pendente.

## Resumo final

Responder de forma curta contendo:

- build/testes;
- migrations;
- runtime/serviços;
- agentes, se houver;
- secret scanning;
- commit criado;
- pull/rebase/push;
- correções automáticas relevantes;
- estado final do worktree;
- avisos não bloqueantes.
