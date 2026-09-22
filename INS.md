# INS — Preparação e padronização de projeto

## Objetivo

Quando o usuário enviar exatamente `ins`, preparar um projeto que ainda não esteja apto a usar `loc` e `prod`.

O `ins` deve inspecionar primeiro e modificar apenas o necessário para tornar o projeto previsível, testável, versionado e implantável com segurança.

## Autonomia

- Instalar/configurar automaticamente o que for comum, seguro e necessário.
- Preservar a estrutura existente.
- Evitar refactors desnecessários.
- Se for necessária mudança estrutural ampla, parar antes, apresentar o plano e aguardar aprovação.

## Ações que sempre exigem aprovação

- Exclusão de arquivos importantes.
- Alteração destrutiva em banco.
- Alteração de portas de VPS.
- Alteração de bancos de VPS.
- Mudança estrutural ampla ou de alto risco.
- Baseline/migration de produção com risco.
- Ações irreversíveis relevantes.

## Inspeção inicial

Detectar automaticamente:

- stack e runtimes;
- estrutura backend/frontend;
- gerenciadores de dependência;
- Git;
- remoto GitHub;
- scripts de start/build/test;
- testes existentes;
- banco;
- migrations;
- configuração de ambiente;
- health checks;
- logs;
- CI;
- documentação;
- estratégia de deploy;
- backup/rollback;
- requisitos ainda ausentes para `loc` e `prod`.

Não inventar valores desconhecidos.

## Git

Se não houver Git:

- inicializar repositório;
- criar `.gitignore` adequado;
- configurar branch principal;
- preparar primeiro commit.

Se não houver remoto:

- criar repositório privado no GitHub quando possível;
- configurar `origin`;
- usar a branch atual/principal;
- não criar remoto duplicado.

Antes de qualquer push:

- executar secret scanning;
- impedir publicação de segredos.

## Dependências e runtimes

Se faltarem ferramentas necessárias ao projeto, instalar automaticamente quando seguro, por exemplo:

- Java;
- Maven/Gradle;
- Node/npm;
- Python;
- PostgreSQL;
- outras dependências realmente exigidas pelo projeto.

Não instalar tecnologia desnecessária.

## Padronização operacional

Quando necessário, criar/ajustar comandos confiáveis para:

- start;
- build;
- test;
- lint;
- validação;
- geração de artefatos.

Preservar convenções existentes sempre que forem adequadas.

## Testes

Se não houver testes:

- criar uma base mínima e segura;
- validar contexto/subida;
- adicionar apenas testes genéricos e claramente suportados;
- não inventar regras de negócio.

## Ambiente e segredos

Se não houver configuração adequada:

- criar estrutura de ambiente segura;
- usar `.env`, properties ou equivalente apropriado;
- nunca versionar segredos reais;
- criar exemplos versionáveis quando útil;
- usar permissões restritas para arquivos locais sensíveis.

## Banco e migrations

Se o projeto usar banco sem migrations:

- configurar ferramenta apropriada;
- para Spring, preferir Flyway;
- banco novo/local: criar migration inicial;
- banco existente com dados: inspecionar schema e criar baseline compatível sem apagar/alterar dados;
- produção: baseline ou migration destrutiva exige aprovação.

## Health e logs

Quando aplicável:

- criar health check mínimo;
- garantir logs suficientes para diagnóstico;
- evitar infraestrutura excessiva.

## CI

Se o projeto estiver no GitHub e não tiver CI:

- criar GitHub Actions básico para build/testes em push e PR;
- não introduzir segredos sensíveis desnecessários.

## Documentação

Criar/atualizar README curto com:

- instalação;
- execução;
- testes;
- dependências;
- variáveis de ambiente;
- deploy básico.

## Deploy

Se não houver estrutura de deploy:

- preparar scripts/templates necessários;
- preparar releases, backup e rollback quando aplicável;
- preparar configuração de serviço, como systemd, quando necessário;
- não executar deploy ainda.

Se faltar informação crítica de produção, pedir somente os dados realmente necessários, por exemplo:

- host/IP;
- usuário SSH;
- porta;
- diretório remoto;
- nome do serviço;
- domínio/health endpoint.

Nunca inventar esses valores.

## Backup

Antes de liberar `prod`:

- garantir rotina confiável de backup de banco e artefatos;
- garantir estratégia de rollback compatível com o projeto.

## Instruções do Codex

Criar/atualizar `AGENTS.md` do projeto contendo:

- significado de `loc`;
- significado de `prod`;
- significado de `ins`;
- comandos reais do projeto;
- arquitetura relevante;
- regras de segurança;
- limites de autonomia;
- particularidades do projeto.

O `AGENTS.md` deve complementar estes workflows globais, não duplicá-los desnecessariamente.

## Validação final

Ao terminar a preparação:

1. executar uma validação equivalente ao `loc`;
2. corrigir automaticamente problemas simples e seguros;
3. confirmar build/testes/runtime;
4. confirmar secret scanning;
5. confirmar Git;
6. só considerar o projeto pronto quando `loc` puder ser usado de forma confiável.

## Git final

Após toda a validação:

- gerar mensagem de commit automaticamente baseada no diff;
- criar commit com as configurações realizadas;
- pull/rebase quando necessário;
- push;
- deixar o projeto pronto para `loc` e, quando a infraestrutura permitir, `prod`.

## Resumo final

Responder de forma curta contendo:

- o que foi instalado;
- o que foi configurado;
- arquivos criados/alterados;
- testes/validações executados;
- o que já está pronto;
- o que ainda depende do usuário;
- se `loc` está liberado;
- se `prod` está liberado.
