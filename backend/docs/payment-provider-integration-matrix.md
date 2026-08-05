# Matriz oficial de integração com terminais de pagamento

Consulta realizada em **2026-07-16**. Foram usadas somente páginas oficiais dos próprios providers. Esta matriz não autoriza integração em produção e não transforma nenhum provider real em disponível no Vitrine 7.

## Resumo

| Provider | Produto/modelo oficial confirmado | Execução e transporte | Acesso, equipamento e homologação | Backend-only | Status real no Vitrine 7 |
|---|---|---|---|---|---|
| Stone | SDK Android Stone para POS Android ou Pinpad Bluetooth | Aplicativo Android no SmartPOS ou dispositivo Android conectado; SDK nativo e Bluetooth conforme o hardware | Programa de parcerias, credencial de integração e token privado do repositório; sandbox; roteiro de homologação; equipamento suportado para o fluxo físico | Não para captura presencial via SDK; requer agente/aplicativo nativo | `OFFICIAL_DOCUMENTATION_CONFIRMED`, `COMMERCIAL_ACCESS_REQUIRED`, `HARDWARE_REQUIRED`, `NATIVE_AGENT_REQUIRED`, `IMPLEMENTATION_BLOCKED` |
| PagBank | SmartPOS/PlugPag; SDK Java/Kotlin embarcado no terminal | Aplicativo Android no SmartPOS aciona o SDK e recebe resultado por listener; SmartPOS não usa TEF | Contato comercial e conta avançada; terminal DEBUG fornecido pelo PagBank; APK e evidências para homologação; distribuição por loja/reseller | Não para SmartPOS; requer agente/aplicativo nativo | `OFFICIAL_DOCUMENTATION_CONFIRMED`, `COMMERCIAL_ACCESS_REQUIRED`, `HARDWARE_REQUIRED`, `NATIVE_AGENT_REQUIRED`, `IMPLEMENTATION_BLOCKED` |
| Cielo | Cielo Smart/LIO Order Manager: SDK local, deep link e modelo remoto | SDK/deep link executado no app Android embarcado; no modelo remoto o backend gerencia ordens, mas o pagamento ocorre localmente na Cielo Smart/LIO | Conta no portal, Client ID/access token, emulador para desenvolvimento, certificação e equipamento/terminal para produção | O modelo remoto admite backend para ordens/notificações, mas a captura presencial continua no terminal; não é integração exclusivamente backend | `OFFICIAL_DOCUMENTATION_CONFIRMED`, `COMMERCIAL_ACCESS_REQUIRED`, `HARDWARE_REQUIRED`, `NATIVE_AGENT_REQUIRED`, `IMPLEMENTATION_BLOCKED` |
| C6 | C6 Pay/TEF e TEF PayGo divulgados com API, DLL, troca de arquivos e Android; portal do desenvolvedor condicionado a cadastro | O site confirma alternativas de TEF/API/DLL/Android, mas a especificação técnica aplicável ao terminal do estabelecimento não está publicamente acessível sem credenciamento | Cadastro no portal, credenciais por e-mail, avaliação de evidências, termo de responsabilidade e liberação de produção; equipamento depende do produto escolhido | Não presumido para a maquininha; produto e transporte precisam ser definidos comercialmente | `OFFICIAL_DOCUMENTATION_CONFIRMED`, `COMMERCIAL_ACCESS_REQUIRED`, `HARDWARE_REQUIRED`, `NATIVE_AGENT_REQUIRED`, `IMPLEMENTATION_BLOCKED` |

## Fontes oficiais consultadas

### Stone

- [O que é a SDK Android Stone](https://sdkandroid.stone.com.br/docs/o-que-e-a-sdk-android): confirma pagamentos em solução Android junto de Pinpad ou POS.
- [Getting Started do SDK Android](https://sdkandroid.stone.com.br/reference/preparando-aplicacao): confirma dependências Android/POS e token privado do repositório fornecido no credenciamento.
- [Dispositivos suportados](https://sdkandroid.stone.com.br/v4.9.2/reference/dispositivos-suportados): confirma POS Android e Pinpad Bluetooth suportados.
- [Processo de integração](https://sdkandroid.stone.com.br/reference/processo-de-integracao): confirma programa de parcerias, sandbox, credenciais, homologação e produção.

Conclusão: a SDK é nativa Android e o fluxo físico depende do dispositivo. O backend Spring pode orquestrar e registrar, mas não substituir o código executado junto ao POS/Pinpad.

### PagBank

- [Integração SmartPOS](https://developer.pagbank.com.br/docs/integracao-smartpos-1): confirma contato comercial, terminal DEBUG, SDK, homologação e produção.
- [Desenvolvimento SmartPOS](https://developer.pagbank.com.br/docs/desenvolvimento-smartpos): confirma implementação Java/Kotlin nativa Android.
- [FAQ SmartPOS](https://developer.pagbank.com.br/docs/faq-smartpos): confirma conta avançada/parceria, SDK Java/Kotlin, terminal DEBUG, homologação e que SmartPOS não se comunica por TEF.
- [Homologação SmartPOS](https://developer.pagbank.com.br/docs/homologacao-smartpos): confirma cadastro comercial obrigatório e avaliação do APK/evidências.

Conclusão: a aplicação que chama PlugPag/SDK deve rodar no terminal Android. Não existe base oficial para chamar esse SDK diretamente do backend Spring.

### Cielo

- [Manual Cielo Smart/LIO](https://developercielo.github.io/manual/cielo-lio): confirma integração por SDK local/deep link e integração remota de ordens.
- [Manual de integração local](https://developercielo.github.io/manual/lio-local): confirma app Android, Client ID/access token, emulador, Cielo Store e certificação.
- [FAQ Cielo LIO](https://developercielo.github.io/en/faq/faq-cielo-lio): confirma credenciais de teste, emulador e tokens de produção condicionados ao portal/suporte.

Conclusão: mesmo na integração remota, a ordem é paga localmente na Cielo Smart/LIO. O bridge é adequado para um futuro agente nativo; nenhuma credencial Cielo deve ser enviada na fila.

### C6

- [APIs e integrações C6 Bank](https://www.c6bank.com.br/apis-integracao/): confirma C6 Pay, TEF, TEF PayGo, alternativas API/DLL/Android e o processo de cadastro, credenciais, evidências e liberação.
- [Manual oficial C6 Pay Smart](https://www.c6bank.com.br/files/manual-digital-c6pay-smart.pdf): confirma o equipamento C6 Pay Smart e seu uso operacional, sem expor um contrato técnico público suficiente para implementar o adapter.

Conclusão: existe oferta oficial de integração, mas o contrato aplicável e as credenciais dependem do portal/relacionamento comercial. Não foi inventada uma API de pagamento presencial.

## Decisão para o Passo 12B

- `SIMULATOR` permanece o único provider `AVAILABLE` e operacional.
- `PAGBANK` permanece `IMPLEMENTATION_PENDING`.
- `STONE`, `CIELO` e `C6` não fazem parte do catálogo administrativo atual.
- O backend implementa apenas o protocolo seguro e normalizado com o futuro agente nativo.
- SDKs, credenciais e segredos do provider não trafegam pela fila do bridge.
- A escolha e implementação do primeiro adapter real ficam condicionadas ao provider efetivamente contratado, hardware disponível, credenciais de homologação e documentação oficial aplicável.
