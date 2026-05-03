# Prompt — Refatoração Completa do Agente de Impressão para Modelo por Agente + Terminais

## Objetivo

Refatorar o **agente de impressão** para sair do modelo antigo baseado em:

```text
idCaixa + chaveAcesso + impressora detectada automaticamente
```

e evoluir para o novo modelo baseado em:

```text
chave do agente
+
terminais vinculados no backend
+
impressora indicada pelo job
```

O objetivo final é que o agente seja simples para o cliente instalar e operar:

```text
baixar agente
instalar com Next → Next → Finish
agente ativa/usa a chave gerada pelo backend
front principal do sistema define os terminais e impressoras
front local do agente fica apenas para suporte técnico
```

Este prompt é para a **refatoração do agente**, principalmente o **back local do agente**.

O agente deve continuar sendo simples, confiável e bem documentado.

---

## 0. Instrução inicial obrigatória

Antes de alterar qualquer código:

1. Ler o `CLAUDE.md` ou arquivo equivalente de instruções do projeto.
2. Ler a documentação atual do agente, se existir:
   - `agente-impressao-mapeamento.md`
   - `front-agente-mapeamento.md`
   - documentação sobre terminal de impressão
   - documentação sobre impressão de cozinha/produção
3. Mapear o código atual:
   - scheduler;
   - orquestrador;
   - client HTTP para o backend;
   - DTO de próximo job;
   - DTO de confirmação;
   - SQLite/configuração local;
   - motor de impressão;
   - detector/listagem de impressoras;
   - controllers locais;
   - frontend estático integrado;
   - logs;
   - status.
4. Não alterar código antes de entender o fluxo atual.
5. Não quebrar o agente atual sem uma estratégia clara de migração.
6. Ao final, documentar tudo em `.md`.

---

## 1. Contexto atual

Hoje o agente funciona assim:

```text
1. Scheduler executa ciclo.
2. Agente lê config local no SQLite:
   - idCaixa
   - chaveAcesso
   - apiBaseUrl
   - intervaloMs
3. Agente chama:
   GET {apiBaseUrl}/proximo?idCaixa={idCaixa}&chaveAcesso={chaveAcesso}
4. Backend retorna um job de impressão.
5. Agente decodifica Base64.
6. Agente escolhe impressora automaticamente:
   - tenta achar impressora com nome contendo "Elgin";
   - senão usa a primeira impressora disponível.
7. Agente imprime.
8. Agente confirma:
   POST {apiBaseUrl}/confirma
```

Problemas do modelo atual:

```text
- idCaixa mistura conceito financeiro com impressão.
- Cozinha/bar/chapa não deveriam ser "caixas falsos".
- O agente não suporta bem múltiplas impressoras.
- O campo nomeImpressora já vem no payload, mas hoje é ignorado.
- O usuário precisa copiar/colar ID do caixa, chave e URL.
- O intervalo de polling é manual e pode ser configurado errado.
- O front local do agente é usado por cliente final, mas deveria ser apenas técnico.
```

---

## 2. Novo desenho conceitual

### 2.1 Agente de Impressão

Representa o executável instalado em um computador.

Cada agente baixado pelo sistema possui uma chave única.

Exemplo:

```text
Agente Principal
chave = xxx-xxxx-xxxx

Agente Cozinha
chave = yyy-yyyy-yyyy
```

O agente instalado localmente guarda apenas sua chave e configurações técnicas.

### 2.2 Terminal de Impressão

Representa o destino lógico da impressão.

Exemplos:

```text
Terminal: Caixa
Impressora Windows: ELGIN-CAIXA
Largura: 58mm

Terminal: Cozinha
Impressora Windows: EPSON-COZINHA
Largura: 58mm

Terminal: Bar
Impressora Windows: BEMATECH-BAR
Largura: 80mm
```

### 2.3 Amarração agente x terminal

O backend principal define quais terminais cada agente atende.

Exemplo com um único agente para tudo:

```text
Agente xxx-xxxx-xxxx atende:
- Caixa
- Cozinha
- Bar
```

Exemplo com agentes separados:

```text
Agente xxx-xxxx-xxxx atende:
- Caixa

Agente yyy-yyyy-yyyy atende:
- Cozinha

Agente zzz-zzzz-zzzz atende:
- Bar
```

Com isso, o agente local não precisa saber se é caixa, cozinha ou bar.

Ele só sabe:

```text
minha chave é xxx-xxxx-xxxx
```

O backend decide quais jobs aquela chave pode consumir.

---

## 3. Regra principal da refatoração

Eliminar a necessidade de o agente operar por:

```text
idCaixa
idTerminalImpressao
modo Caixa
modo Terminal
```

O modelo final deve ser:

```text
Agente consulta fila pela chave do agente.
Backend retorna jobs dos terminais vinculados a essa chave.
Job informa nomeImpressora e larguraPapelMm.
Agente imprime na impressora informada.
```

---

## 4. Novo fluxo alvo

```text
Agente inicia
↓
Lê configuração local no SQLite
↓
Obtém chave do agente
↓
Chama backend:
    GET {apiBaseUrl}/agente/proximo
    Header X-Agente-Key: xxx-xxxx-xxxx
↓
Backend valida chave
↓
Backend identifica terminais vinculados ao agente
↓
Backend retorna próximo job desses terminais
↓
Agente lê:
    idJob
    tipoDocumento
    terminalNome
    nomeImpressora
    larguraPapelMm
    conteudo Base64
↓
Agente localiza impressora no Windows
↓
Imprime
↓
Confirma:
    POST {apiBaseUrl}/agente/confirma
    Header X-Agente-Key: xxx-xxxx-xxxx
```

---

## 5. Endpoints esperados do backend principal

### 5.1 Buscar próximo job por chave do agente

Preferencialmente:

```http
GET {apiBaseUrl}/agente/proximo
Header: X-Agente-Key: xxx-xxxx-xxxx
```

Alternativa temporária, se o backend ainda não estiver com header:

```http
GET {apiBaseUrl}/agente/proximo?chaveAgente=xxx-xxxx-xxxx
```

Mas a implementação final deve preferir header.

### 5.2 Confirmar job

```http
POST {apiBaseUrl}/agente/confirma
Header: X-Agente-Key: xxx-xxxx-xxxx
Content-Type: application/json
```

Body:

```json
{
  "idJob": 123,
  "status": "OK",
  "mensagemErro": null,
  "nomeImpressoraUsada": "EPSON-COZINHA",
  "dataHoraLocal": "2026-05-03T10:42:01"
}
```

Em caso de erro:

```json
{
  "idJob": 123,
  "status": "ERRO",
  "mensagemErro": "Impressora EPSON-COZINHA não encontrada no Windows.",
  "nomeImpressoraUsada": null,
  "dataHoraLocal": "2026-05-03T10:42:01"
}
```

---

## 6. Novo DTO esperado do próximo job

O agente deve suportar um response contendo:

```json
{
  "idJob": 123,
  "idTerminalImpressao": 2,
  "terminalNome": "Cozinha",
  "tipoDocumento": "PEDIDO_PRODUCAO",
  "conteudo": "BASE64_ESC_POS",
  "nomeImpressora": "EPSON-COZINHA",
  "larguraPapelMm": 58,
  "proximoPollingMs": 1000
}
```

Campos obrigatórios para imprimir:

```text
idJob
tipoDocumento
conteudo
```

Campos importantes para roteamento:

```text
nomeImpressora
larguraPapelMm
idTerminalImpressao
terminalNome
```

Se `nomeImpressora` vier nulo, usar fallback local.

---

## 7. Configuração local do agente

### 7.1 Configuração final desejada

O agente local deve guardar no SQLite:

```text
apiBaseUrl
chaveAgente
intervaloMinimoMs
intervaloPadraoMs
intervaloBaixoPicoMs
intervaloMaximoMs
intervaloErroMs
impressoraFallback
larguraPapelPadraoMm
usarNomeImpressoraDoJob
modoTecnicoHabilitado
```

### 7.2 Campos removidos do fluxo normal

O cliente comum não deve configurar:

```text
idCaixa
idTerminalImpressao
intervaloMs manual
```

Esses campos podem existir temporariamente para migração, mas não devem fazer parte do novo fluxo principal.

### 7.3 URL default

A URL default do agente deve ser:

```text
https://api.zseposmei.cloud/posmei-api/api/posmei/impressao
```

Mas ela precisa continuar editável em modo técnico, para testes locais.

Exemplo para teste local:

```text
http://127.0.0.1:8080/posmei-api/api/posmei/impressao
```

---

## 8. Estratégia de configuração inicial / bootstrap

### 8.1 Objetivo

O usuário final não deve precisar copiar:

```text
idCaixa
chave
URL
```

O ideal é que, ao baixar o agente pelo sistema web, venha junto uma configuração com a chave do agente.

### 8.2 Modelo simples para MVP

O instalador ou pacote do agente pode vir com um arquivo:

```text
agent-bootstrap.json
```

Exemplo:

```json
{
  "apiBaseUrl": "https://api.zseposmei.cloud/posmei-api/api/posmei/impressao",
  "chaveAgente": "xxx-xxxx-xxxx"
}
```

No primeiro start:

```text
1. Agente procura agent-bootstrap.json.
2. Se existir, lê apiBaseUrl e chaveAgente.
3. Salva no SQLite.
4. Marca configuração como inicializada.
5. Opcionalmente move/renomeia o arquivo para evitar reutilização acidental.
```

### 8.3 Modelo recomendado futuro

Usar `tokenInstalacao` temporário:

```json
{
  "apiBaseUrl": "https://api.zseposmei.cloud/posmei-api/api/posmei/impressao",
  "tokenInstalacao": "token-temporario"
}
```

O agente ativa no backend e recebe a chave definitiva.

Para esta etapa, implementar o modelo simples se o backend ainda não tiver ativação por token.

---

## 9. Polling inteligente

### 9.1 Problema atual

Hoje o tempo de polling é manual. Isso não deve ficar editável para o cliente.

### 9.2 Nova regra

Remover o intervalo manual da tela comum.

O agente deve usar polling adaptativo.

Valores definidos:

| Situação | Valor |
|---|---:|
| Pico / job encontrado | `1000 ms` = 1s |
| Operação normal | `3000 ms` = 3s |
| Baixo pico | `10000 ms` = 10s |
| Ocioso prolongado | `15000 ms` = 15s |
| Erro de conexão | `15000 ms` = 15s |
| Teto absoluto | `15000 ms` = 15s |

Não usar valores acima de 15 segundos para impressão.

### 9.3 Regra adaptativa

```text
Se encontrou job:
    próximo polling = 1000ms
    zera contador de ciclos vazios

Se não encontrou job 1 vez:
    próximo polling = 3000ms

Se não encontrou job 3 vezes:
    próximo polling = 10000ms

Se não encontrou job 6 vezes ou mais:
    próximo polling = 15000ms

Se ocorreu erro de conexão:
    próximo polling = 15000ms
```

### 9.4 Se backend retornar proximoPollingMs

Se o backend retornar `proximoPollingMs`, o agente pode respeitar, mas deve limitar:

```text
mínimo = 1000ms
máximo = 15000ms
```

Exemplo:

```text
backend retornou 500ms → agente usa 1000ms
backend retornou 30000ms → agente usa 15000ms
```

### 9.5 Status exposto ao front local

O endpoint local de status deve retornar:

```json
{
  "status": "RODANDO",
  "ultimaExecucao": "2026-05-03T10:42:01",
  "ultimaMensagem": "Ciclo executado com sucesso",
  "proximoPollingMs": 3000,
  "proximaConsultaEm": "2026-05-03T10:42:04",
  "ciclosSemJob": 2,
  "ultimoJobId": 123,
  "ultimoTipoDocumento": "PEDIDO_PRODUCAO",
  "ultimaImpressoraUsada": "EPSON-COZINHA"
}
```

---

## 10. Seleção de impressora

### 10.1 Problema atual

Hoje o agente ignora `nomeImpressora` do job e usa lógica automática:

```text
procurar "Elgin"
senão primeira impressora
```

Isso precisa ser substituído.

### 10.2 Nova regra

Ordem de decisão:

```text
1. Se job.nomeImpressora vier preenchido:
      tentar imprimir exatamente nessa impressora.

2. Se não encontrar correspondência exata:
      tentar correspondência normalizada:
          trim
          case-insensitive
          remover espaços duplicados

3. Se ainda não encontrar:
      se impressoraFallback estiver configurada:
          tentar fallback.

4. Se fallback não existir:
      tentar impressora padrão do Windows apenas se permitirFallbackSistema = true.

5. Se nenhuma impressora for encontrada:
      confirmar job como ERRO.
```

### 10.3 Recomendação de segurança

Para produção, o ideal é:

```text
usar impressora do job
senão fallback configurado
senão ERRO claro
```

Evitar imprimir silenciosamente na primeira impressora disponível.

A primeira impressora do Windows só deve ser usada em modo técnico ou compatibilidade.

### 10.4 Log obrigatório

Para cada job, registrar:

```text
idJob
tipoDocumento
terminalNome
nomeImpressoraSolicitada
nomeImpressoraUsada
larguraPapelMm
status OK/ERRO
mensagemErro
```

---

## 11. Motor de impressão

Refatorar o motor para aceitar explicitamente a impressora.

Assinatura sugerida:

```java
void printRawBytes(byte[] bytes, String jobName, String nomeImpressora, Integer larguraPapelMm);
```

Ou usando um objeto:

```java
PrintJobContext {
    Long idJob;
    String tipoDocumento;
    String terminalNome;
    String nomeImpressoraSolicitada;
    Integer larguraPapelMm;
}
```

Evitar que o `MotorImpressao` decida sozinho a impressora sem contexto.

Criar ou refatorar serviço:

```text
PrinterResolver
```

Responsável por:

```text
listar impressoras
buscar por nome exato
buscar por nome normalizado
buscar fallback
validar existência
retornar erro claro
```

---

## 12. Confirmação de impressão

A confirmação deve continuar enviando OK/ERRO.

Adicionar, se o backend aceitar:

```text
nomeImpressoraUsada
dataHoraLocal
```

Não deixar job sem confirmação se a impressão falhar por impressora ausente.

Cenários:

```text
job impresso com sucesso:
    confirmar OK

impressora não encontrada:
    confirmar ERRO

Base64 inválido:
    confirmar ERRO

erro no spooler:
    confirmar ERRO

erro HTTP na confirmação:
    logar erro e tentar confirmar novamente em ciclo posterior, se possível
```

Se ainda não existir mecanismo local de confirmação pendente, documentar como pendência.

---

## 13. Suporte a uma ou várias impressoras

### 13.1 Um agente para tudo

Se vários terminais estiverem vinculados à mesma chave:

```text
Agente xxx atende:
- Caixa
- Cozinha
- Bar
```

O backend pode retornar jobs alternados de cada terminal.

O agente imprime cada job na impressora informada.

### 13.2 Um agente por setor

Se cada setor tiver uma chave própria:

```text
Agente Cozinha atende:
- Terminal Cozinha

Agente Bar atende:
- Terminal Bar
```

O backend só retorna jobs do terminal vinculado à chave.

### 13.3 O agente não precisa saber o desenho

O agente não precisa saber se atende um ou vários terminais.

Ele apenas:

```text
consulta pela chave
recebe job
imprime na impressora informada
confirma
```

---

## 14. Front local do agente

O front local do agente deve virar painel técnico.

### 14.1 Não deve ser foco do usuário comum

O fluxo principal de configuração deve ficar no front principal do sistema.

O front local deve servir para suporte:

```text
status
logs
impressoras detectadas
teste de impressão
teste de conexão
configuração técnica
reset
```

### 14.2 Campos que devem sair da tela comum

Remover/ocultar da tela principal:

```text
idCaixa
intervaloMs manual
```

### 14.3 Campos técnicos

Em modo técnico, pode mostrar:

```text
API Base URL
Chave do agente
Impressora fallback
Largura padrão do papel
Usar nome da impressora do job
Polling atual
Próxima consulta
```

A chave deve ser exibida como password/masked:

```text
••••••••••••
```

com botão mostrar/ocultar.

### 14.4 Card de status

Exibir:

```text
Status do agente
Última consulta
Próxima consulta
Último job
Última impressora usada
Quantidade de ciclos sem job
API conectada/não conectada
```

### 14.5 Card de impressoras

Exibir todas as impressoras detectadas.

Para cada impressora:

```text
nome
padrão do Windows
status
driver
porta
```

Se houver jobs recentes, mostrar se a impressora solicitada foi encontrada ou não.

### 14.6 Teste de impressão

Permitir teste em:

```text
impressora selecionada
impressora fallback
impressora informada manualmente em modo técnico
```

Não testar sempre na impressora padrão se existe fallback configurado.

---

## 15. SQLite

### 15.1 Manter tabelas atuais quando possível

Não apagar tabelas antigas sem migração.

Atualizar `agent_config` para novas chaves:

```text
apiBaseUrl
chaveAgente
impressoraFallback
larguraPapelPadraoMm
usarNomeImpressoraDoJob
pollingMinimoMs
pollingNormalMs
pollingBaixoPicoMs
pollingMaximoMs
pollingErroMs
configInicializada
```

### 15.2 Compatibilidade/migração

Se existir config antiga com:

```text
idCaixa
chaveAcesso
```

Não quebrar o start.

Fazer uma destas opções:

### Opção A — Migração assistida

```text
Se chaveAgente não existir e chaveAcesso existir:
    usar chaveAcesso como chaveAgente temporária
    registrar log de compatibilidade
    ocultar idCaixa no novo fluxo
```

### Opção B — Bloquear e pedir reinstalação

```text
Se não existir chaveAgente:
    status = CONFIGURACAO_PENDENTE
    mensagem = "Agente não ativado. Baixe novamente o agente pelo sistema."
```

Escolher a opção mais segura conforme o estado do backend.

Como existem poucos clientes em produção, é aceitável documentar que haverá migração via script/back e que o agente novo espera `chaveAgente`.

---

## 16. Logs e diagnóstico

Melhorar logs para que suporte consiga entender:

```text
Agente iniciado
Configuração carregada
Chave do agente encontrada
API Base URL usada
Polling atual
Job recebido
Terminal do job
Tipo do documento
Impressora solicitada
Impressora encontrada
Impressora usada
Impressão OK
Impressão ERRO
Confirmação OK
Confirmação ERRO
```

Não logar chave completa.

Exibir mascarado:

```text
xxx-****-xxxx
```

---

## 17. Status local do agente

Atualizar `AgentStateDTO` para conter visão clara:

```text
status
ultimaExecucao
ultimaMensagem
apiBaseUrl
agenteConfigurado
chaveAgenteMascarada
proximoPollingMs
proximaConsultaEm
ultimoJobId
ultimoTipoDocumento
ultimoTerminal
ultimaImpressoraSolicitada
ultimaImpressoraUsada
ultimoErro
```

Não expor chave completa no status.

---

## 18. Segurança

Regras:

```text
Não expor chave completa no front.
Não logar chave completa.
Não enviar chave em query string se for possível usar header.
Não salvar token temporário após ativação.
Não deixar URL local como default de produção.
Não usar primeira impressora disponível silenciosamente em produção.
```

---

## 19. Documentação obrigatória

Ao final, criar/atualizar documentação em `.md`.

Arquivo sugerido:

```text
docs/agente-back-funcionamento.md
```

ou, se o projeto usa pasta `skils`:

```text
skils/impressao/agente-back-funcionamento.md
```

A documentação deve explicar claramente:

```text
1. O que é o agente.
2. Como o agente inicia.
3. Como lê configuração.
4. Como funciona a chave do agente.
5. Como consulta o backend.
6. Como o backend decide quais jobs retornar.
7. Como o agente escolhe a impressora.
8. Como funciona um agente para várias impressoras.
9. Como funciona um agente por setor.
10. Como funciona o polling inteligente.
11. Como o agente confirma OK/ERRO.
12. Como os logs funcionam.
13. Como o front local deve ser usado.
14. Como testar localmente.
15. Como instalar em produção.
16. Quais configurações ficam no SQLite.
17. Quais endpoints locais existem.
18. Quais endpoints remotos são consumidos.
19. Quais cenários de erro existem.
20. Como diagnosticar problemas.
```

---

## 20. Documento técnico final esperado

O documento precisa ter uma visão clara e completa do agente back.

Estrutura sugerida:

```md
# Agente de Impressão — Funcionamento do Back Local

## 1. Visão geral
## 2. Arquitetura
## 3. Fluxo completo
## 4. Configuração local SQLite
## 5. Chave do agente
## 6. Bootstrap de instalação
## 7. Polling inteligente
## 8. Comunicação com backend
## 9. DTOs
## 10. Seleção de impressora
## 11. Suporte a múltiplas impressoras
## 12. Confirmação de impressão
## 13. Logs
## 14. Status local
## 15. Front local técnico
## 16. Cenários de instalação
## 17. Cenários de erro
## 18. Testes obrigatórios
## 19. Compatibilidade e migração
## 20. Arquivos principais do código
```

---

## 21. Critérios de aceite

A refatoração só deve ser considerada concluída se:

```text
[ ] Agente consegue iniciar com chaveAgente.
[ ] apiBaseUrl default é produção.
[ ] apiBaseUrl continua editável em modo técnico.
[ ] Agente consulta backend pela chave do agente.
[ ] Agente não depende mais de idCaixa no fluxo principal.
[ ] Agente usa nomeImpressora recebido no job.
[ ] Agente usa fallback se nomeImpressora vier vazio.
[ ] Agente não imprime na primeira impressora silenciosamente em produção.
[ ] Agente confirma ERRO se impressora solicitada não existir.
[ ] Agente suporta jobs de terminais diferentes na mesma chave.
[ ] Agente imprime em impressoras diferentes conforme job.
[ ] Polling adaptativo funciona com teto de 15000ms.
[ ] Intervalo manual não fica disponível para cliente comum.
[ ] Logs mostram terminal, tipoDocumento e impressora usada.
[ ] Status local mostra próxima consulta e último job.
[ ] Front local não expõe chave completa.
[ ] Documentação final foi criada.
[ ] Nenhum segredo foi exposto em logs/documentação.
```

---

## 22. Cenários de teste obrigatórios

### Cenário 1 — Start com chave válida

```text
Dado agente com chaveAgente configurada
Quando iniciar
Então status deve ser RODANDO
E deve consultar backend pela chave
```

### Cenário 2 — Sem configuração

```text
Dado agente sem chaveAgente
Quando iniciar
Então status deve ser CONFIGURACAO_PENDENTE
E deve orientar baixar/ativar agente pelo sistema
```

### Cenário 3 — Job com impressora existente

```text
Dado job com nomeImpressora = EPSON-COZINHA
E essa impressora existe no Windows
Quando imprimir
Então deve usar EPSON-COZINHA
E confirmar OK
```

### Cenário 4 — Job com impressora inexistente

```text
Dado job com nomeImpressora = EPSON-COZINHA
E essa impressora não existe no Windows
Quando imprimir
Então deve confirmar ERRO
E logar "Impressora EPSON-COZINHA não encontrada"
```

### Cenário 5 — Job sem nomeImpressora

```text
Dado job sem nomeImpressora
E impressoraFallback configurada
Quando imprimir
Então deve usar fallback
```

### Cenário 6 — Agente único com várias impressoras

```text
Dado a chave do agente atende Caixa, Cozinha e Bar
Quando o backend retornar jobs alternados
Então o agente deve imprimir cada job na impressora informada
```

### Cenário 7 — Polling em pico

```text
Dado agente encontra jobs em sequência
Então próximo polling deve ser 1000ms
```

### Cenário 8 — Polling em baixo movimento

```text
Dado agente não encontra jobs por vários ciclos
Então polling deve subir para 3000ms, depois 10000ms e depois 15000ms
```

### Cenário 9 — Erro de conexão

```text
Dado backend indisponível
Quando ciclo falhar
Então próximo polling deve ser 15000ms
E status deve mostrar erro de conexão
```

### Cenário 10 — Chave mascarada

```text
Dado front local aberto
Então chave do agente deve aparecer mascarada
E logs não devem exibir chave completa
```

---

## 23. Não fazer

Não fazer:

```text
- Não criar modo Caixa/Terminal.
- Não exigir idCaixa no novo fluxo.
- Não exigir idTerminalImpressao no agente local.
- Não deixar cliente editar polling manual.
- Não usar localhost como default de produção.
- Não ignorar nomeImpressora do job.
- Não imprimir silenciosamente na primeira impressora disponível.
- Não logar chave completa.
- Não quebrar endpoint local de status/logs/config.
- Não remover compatibilidade sem documentar migração.
- Não alterar backend principal neste repositório, se ele não fizer parte do agente.
```

---

## 24. Entrega final

Ao final, entregar relatório com:

```text
1. Arquivos alterados.
2. Classes criadas.
3. Classes refatoradas.
4. Novas configurações SQLite.
5. Endpoints locais alterados.
6. Endpoints remotos consumidos.
7. Como funciona o novo polling.
8. Como funciona a escolha da impressora.
9. Como funciona agente único para várias impressoras.
10. Como funciona agente por setor.
11. Como testar localmente.
12. Documentação criada.
13. Pendências que dependem do backend principal.
14. Confirmação de build/testes.
```
