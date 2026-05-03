# Agente de Impressão — Funcionamento do Back Local

**Versão:** 2.0 (modelo chaveAgente + terminais vinculados)
**Data:** 2026-05-03

---

## 1. Visão Geral

O agente é um processo Spring Boot (porta 9090) instalado no computador do cliente. Ele consulta periodicamente o backend principal, recebe jobs de impressão e os envia para impressoras Windows via Java Print Service.

O cliente instala o agente e o backend define quais terminais (e quais impressoras) aquele agente atende. O usuário final não precisa configurar nada além de baixar o instalador.

---

## 2. Arquitetura

```
[Backend principal]
    ↕ HTTP
[Agente Spring Boot — porta 9090]
    ├── Scheduler (polling adaptativo)
    ├── Orquestrador (fluxo completo)
    ├── ImpressaoApiClientBack (HTTP para backend)
    ├── PrinterResolver (seleção de impressora)
    ├── MotorImpressao (envio para spooler Windows)
    ├── ConfigServiceSQLite (agent.db)
    └── Frontend estático (painel técnico)
```

---

## 3. Fluxo Completo

```
ImpressaoScheduler (@Scheduled fixedDelay=1000ms — sem Thread.sleep interno)
  └─ verifica: now >= proximaExecucao?
       ├─ não → retorna sem executar ciclo
       └─ sim → executa ciclo:
              AgentStateService.marcarRodando()
              OrquestradorImpressaoServiceImp.executarCiclo()
                   └─ valida: chaveAgente + apiBaseUrl configurados
                   └─ ImpressaoApiClientBack.buscarProximoJob()
                        GET {apiBaseUrl}/agente/proximo
                        Header X-Agente-Key: {chaveAgente}
                   └─ Se job == null → return null (sem job)
                   └─ Monta PrintJobContext (idJob, tipoDocumento, terminalNome, nomeImpressora, larguraMm)
                   └─ Base64.decode(job.conteudo) → byte[]
                   └─ MotorImpressao.printRawBytes(bytes, ctx)
                        └─ PrinterResolver.resolver(nomeImpressora)
                             → match exato → match normalizado → fallback config → sistema (se permitido) → ERRO
                        └─ javax.print.PrintService.print()
                        └─ retorna nomeImpressoraUsada
                   └─ ImpressaoApiClientBack.confirmarJob(idJob, "OK", null, nomeImpressoraUsada)
                        POST {apiBaseUrl}/agente/confirma
                        Header X-Agente-Key: {chaveAgente}
                   └─ AgentStateService.atualizarUltimoJob(...)
                   └─ return job.proximoPollingMs (ou 0)
              Scheduler recalcula próximo intervalo adaptativo
              proximaExecucao = now + proximoPollingMs
```

---

## 4. Configuração Local SQLite

Arquivo: `agent.db` na pasta de execução do JAR.

Tabela `agent_config` — chave/valor:

| Chave | Padrão | Descrição |
|---|---|---|
| `apiBaseUrl` | `https://api.zseposmei.cloud/posmei-api/api/posmei/impressao` | URL do backend principal |
| `chaveAgente` | `` (vazio) | Chave única do agente — preenchida via bootstrap ou painel técnico |
| `impressoraFallback` | `` (vazio) | Nome da impressora fallback se o job não informar nomeImpressora |
| `larguraPapelPadraoMm` | `80` | Largura padrão do papel quando o job não informa |
| `usarNomeImpressoraDoJob` | `true` | Se true, usa o nomeImpressora recebido no job |
| `permitirFallbackSistema` | `false` | Se true, usa a impressora padrão do Windows como último recurso |
| `modoTecnicoHabilitado` | `false` | Habilita campos avançados no painel local |
| `configInicializada` | `false` | Marcado como true após bootstrap bem-sucedido |

Campos de compatibilidade (migração do modelo antigo):

| Chave | Observação |
|---|---|
| `idCaixa` | Ignorado no novo fluxo — mantido para não quebrar agentes antigos |
| `chaveAcesso` | **Não deve ser migrado automaticamente para `chaveAgente`** — ver seção 19 |

---

## 5. Chave do Agente

A chave do agente (`chaveAgente`) é o único identificador do agente perante o backend.

- É gerada pelo backend quando o agente é cadastrado no sistema.
- É enviada via `X-Agente-Key` em todas as chamadas HTTP.
- Nunca deve aparecer completa nos logs. Formato mascarado: `xxxx****xxxx`
- O backend decide quais terminais estão vinculados a essa chave.

---

## 6. Bootstrap de Instalação

O instalador ou pacote baixado pelo cliente pode incluir o arquivo:

```
agent-bootstrap.json
```

Conteúdo:
```json
{
  "apiBaseUrl": "https://api.zseposmei.cloud/posmei-api/api/posmei/impressao",
  "chaveAgente": "xxx-xxxx-xxxx"
}
```

Comportamento no primeiro start:
1. O `AgentDatabaseInitializer` procura `agent-bootstrap.json` na pasta de execução.
2. Se existir e `configInicializada == false`, lê e salva no SQLite.
3. Marca `configInicializada = true`.
4. Renomeia o arquivo para `agent-bootstrap.used.json` para evitar reutilização.

---

## 7. Polling Inteligente

### 7.1 Estratégia do scheduler

O `ImpressaoScheduler` **não deve usar `Thread.sleep` dentro do método agendado**. Bloquear a thread do scheduler dificulta o shutdown, o monitoramento e a manutenção.

Estratégia recomendada — `proximaExecucao`:

```
1. @Scheduled(fixedDelay = 1000)
2. Scheduler acorda.
3. Verifica: now >= proximaExecucao?
4. Se não chegou a hora → retorna imediatamente.
5. Se chegou → executa o ciclo completo.
6. Ao final, calcula o próximo intervalo adaptativo.
7. Atualiza: proximaExecucao = now + proximoPollingMs
```

O campo `proximaExecucao` é mantido como variável em memória no scheduler e também persistido no SQLite para exibição no status local.

### 7.2 Tabela de intervalos adaptativos

| Situação | Próximo intervalo |
|---|---:|
| Job processado (pico) | 1.000 ms |
| Sem job — 1 ou 2 ciclos | 3.000 ms |
| Sem job — 3 a 5 ciclos | 10.000 ms |
| Sem job — 6+ ciclos | 15.000 ms |
| Erro de conexão | 15.000 ms |

Se o backend retornar `proximoPollingMs` no job, o agente respeita o valor dentro dos limites:
- Mínimo: 1.000 ms
- Máximo: 15.000 ms

O campo `intervaloMs` manual foi **removido** do fluxo normal. O cliente não configura o intervalo.

---

## 8. Comunicação com Backend

### Buscar próximo job

```http
GET {apiBaseUrl}/agente/proximo
X-Agente-Key: {chaveAgente}
```

Resposta quando há job:
```json
{
  "data": {
    "idJob": 123,
    "idTerminalImpressao": 2,
    "terminalNome": "Cozinha",
    "tipoDocumento": "PEDIDO_PRODUCAO",
    "conteudo": "BASE64_ESC_POS",
    "nomeImpressora": "EPSON-COZINHA",
    "larguraPapelMm": 58,
    "proximoPollingMs": 1000
  },
  "ok": true
}
```

Resposta quando não há job: HTTP 204 ou `data: null`.

### Confirmar job

```http
POST {apiBaseUrl}/agente/confirma
X-Agente-Key: {chaveAgente}
Content-Type: application/json
```

Sucesso:
```json
{
  "idJob": 123,
  "status": "OK",
  "mensagemErro": null,
  "nomeImpressoraUsada": "EPSON-COZINHA",
  "dataHoraLocal": "2026-05-03T11:00:00"
}
```

Erro:
```json
{
  "idJob": 123,
  "status": "ERRO",
  "mensagemErro": "Impressora 'EPSON-COZINHA' não encontrada no Windows.",
  "nomeImpressoraUsada": null,
  "dataHoraLocal": "2026-05-03T11:00:00"
}
```

---

## 9. DTOs

### `ProximoJobResponse`

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `idJob` | Long | Sim | ID único do job |
| `tipoDocumento` | String | Sim | Tipo: CUPOM, PEDIDO_PRODUCAO, etc. |
| `conteudo` | String | Sim | Payload ESC/POS em Base64 |
| `idTerminalImpressao` | Long | Não | ID do terminal de impressão |
| `terminalNome` | String | Não | Nome do terminal (ex: "Cozinha") |
| `nomeImpressora` | String | Não | Nome da impressora Windows |
| `larguraPapelMm` | Integer | Não | Largura do papel em mm |
| `proximoPollingMs` | Integer | Não | Sugestão de próximo intervalo |

### `ConfirmarRequest`

| Campo | Tipo | Descrição |
|---|---|---|
| `idJob` | Long | ID do job confirmado |
| `status` | String | "OK" ou "ERRO" |
| `mensagemErro` | String | Detalhe do erro (null se OK) |
| `nomeImpressoraUsada` | String | Nome real usado na impressão |
| `dataHoraLocal` | LocalDateTime | Timestamp local da impressão |

---

## 10. Seleção de Impressora

O `PrinterResolver` segue esta ordem de decisão:

1. **Match exato** pelo `nomeImpressora` do job
2. **Match normalizado** (trim, case-insensitive, espaços normalizados)
3. **Fallback** configurado em `impressoraFallback` no SQLite
4. **Impressora padrão do Windows** — somente se `permitirFallbackSistema = true`
5. **ERRO claro** — job confirmado como ERRO, log registrado

Para produção, recomenda-se:
- `usarNomeImpressoraDoJob = true`
- `permitirFallbackSistema = false`
- `impressoraFallback` configurado como backup

---

## 11. Suporte a Múltiplas Impressoras

### Um agente para vários terminais

O backend vincula múltiplos terminais à mesma chave. O backend retorna jobs alternados. O agente imprime cada job na impressora informada pelo job.

```
Chave xxx → atende: Caixa (ELGIN-CAIXA), Cozinha (EPSON-COZINHA), Bar (BEMATECH-BAR)
```

### Um agente por setor

Cada setor tem sua própria chave. O backend só retorna jobs do terminal vinculado.

```
Chave xxx → atende: Caixa (ELGIN-CAIXA)
Chave yyy → atende: Cozinha (EPSON-COZINHA)
```

O agente não precisa saber qual cenário está em uso. Ele apenas recebe o job, usa a impressora informada e confirma.

---

## 12. Confirmação de Impressão

| Cenário | Ação |
|---|---|
| Impressão OK | `POST /confirma { status: "OK", nomeImpressoraUsada: "...", ... }` |
| Impressora não encontrada | `POST /confirma { status: "ERRO", mensagemErro: "Impressora X não encontrada" }` |
| Base64 inválido | `POST /confirma { status: "ERRO", mensagemErro: "..." }` |
| Erro no spooler Windows | `POST /confirma { status: "ERRO", mensagemErro: "Falha ao imprimir" }` |
| Erro HTTP na confirmação | Log local — job pode ficar pendente no backend (ver risco abaixo) |

### Confirmação Pendente Local — Risco e Pendência Técnica

**Risco: médio/alto — impacto: possível duplicidade de impressão.**

Se o agente imprimir fisicamente o job, mas falhar ao confirmar no backend por erro HTTP, o backend mantém o job como pendente. O mesmo job pode ser entregue novamente ao agente em um ciclo posterior, gerando reimpressão indesejada.

**Evolução recomendada — tabela `agent_confirmacao_pendente`:**

Criar tabela local no SQLite:

```sql
CREATE TABLE agent_confirmacao_pendente (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    id_job      INTEGER NOT NULL,
    status      TEXT NOT NULL,
    mensagem_erro TEXT,
    nome_impressora_usada TEXT,
    data_hora_local TEXT,
    tentativas  INTEGER DEFAULT 0,
    criado_em   DATETIME DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

Fluxo sugerido:

```
1. Se a confirmação HTTP falhar → salvar em agent_confirmacao_pendente.
2. A cada ciclo, antes de buscar novo job → tentar reenviar confirmações pendentes.
3. Se reenvio OK → remover ou marcar como resolvida.
4. Registrar log claro de cada tentativa.
```

Status: **pendência técnica recomendada para próxima evolução.**

---

## 13. Logs

Eventos registrados em `agent_log` (SQLite) e expostos em `GET /api/agent/logs`:

```
Agente não configurado (chaveAgente ausente). Status: CONFIGURACAO_PENDENTE
Job recebido: #123 | terminal=Cozinha | tipo=PEDIDO_PRODUCAO | impressora=EPSON-COZINHA
Job #123 impresso com sucesso na impressora: EPSON-COZINHA
Erro ao imprimir job #123 | impressora=EPSON-COZINHA → Impressora 'EPSON-COZINHA' não encontrada
Ciclo executado. Próximo polling: 3000ms
```

A chave do agente nunca aparece completa nos logs. Formato mascarado: `xxxx****xxxx`.

---

## 14. Status Local

Endpoint: `GET /api/agent/status`

Retorna `AgentStateDTO`:

```json
{
  "status": "RODANDO",
  "ultimaExecucao": "2026-05-03T11:00:00",
  "ultimaMensagem": "Executando ciclo",
  "apiBaseUrl": "https://api.zseposmei.cloud/...",
  "agenteConfigurado": true,
  "chaveAgenteMascarada": "xxxx****xxxx",
  "proximoPollingMs": 3000,
  "proximaConsultaEm": "2026-05-03T11:00:03",
  "ciclosSemJob": 2,
  "ultimoJobId": 123,
  "ultimoTipoDocumento": "PEDIDO_PRODUCAO",
  "ultimoTerminal": "Cozinha",
  "ultimaImpressoraSolicitada": "EPSON-COZINHA",
  "ultimaImpressoraUsada": "EPSON-COZINHA",
  "ultimoErro": null
}
```

---

## 15. Front Local Técnico

O frontend em `http://127.0.0.1:9090` é um painel de suporte técnico, **não uma tela para o usuário comum**.

O usuário final não precisa abrir este painel. O fluxo de configuração normal é feito pelo sistema web principal. Este painel existe para:
- diagnóstico em campo;
- teste de impressão;
- configuração técnica em caso de problema.

**Tela principal — visível para todos:**

| Elemento | Descrição |
|---|---|
| Card de status | Status do agente (RODANDO / ERRO / CONFIGURACAO_PENDENTE) |
| Última consulta | Data/hora da última vez que o backend foi consultado |
| Próxima consulta | Data/hora prevista para a próxima consulta |
| Último job | ID, tipo do documento e terminal do último job recebido |
| Última impressora usada | Nome real da impressora usada na última impressão |
| Impressoras detectadas | Lista de todas as impressoras disponíveis no Windows |

**O que não deve aparecer na tela comum:**
- `idCaixa` — campo do modelo antigo, não existe no novo fluxo
- Intervalo de polling manual — o cliente não configura isso
- Chave do agente completa — sempre mascarada

**Modo técnico (`modoTecnicoHabilitado = true`):**

| Campo | Comportamento |
|---|---|
| API Base URL | Editável — permite apontar para ambiente local ou de homologação |
| Chave do agente | Exibida mascarada (`xxxx****xxxx`); botão para revelar temporariamente |
| Impressora fallback | Editável — nome exato da impressora Windows usada como backup |
| Largura padrão do papel | Editável em mm |
| Usar nome da impressora do job | Toggle true/false |
| Permitir fallback sistema | Toggle true/false — recomendado false em produção |
| Polling atual | Exibição informativa: próximo intervalo e próxima consulta |
| Testar conexão | Botão para verificar se o backend responde |
| Testar impressão | Envia impressão de teste na impressora fallback configurada |

**Regras de exibição da chave:**
- Sempre mascarada no carregamento.
- Botão "mostrar" revela por 10 segundos e volta a mascarar automaticamente (recomendado).
- Nunca exibir a chave completa em logs, status JSON ou tela comum.

---

## 16. Cenários de Instalação

### Cenário A — Loja simples (1 computador, 1 impressora)

```
1. Baixar instalador + agent-bootstrap.json com chaveAgente
2. Instalar: Next → Next → Finish
3. Agente inicia automaticamente
4. Bootstrap carrega chave
5. Backend retorna jobs para o terminal vinculado
6. Impressora é detectada pelo nome informado no job
```

### Cenário B — Loja com impressoras por setor (1 computador, várias impressoras)

```
1. Mesma instalação do Cenário A
2. No sistema web, vincular todos os terminais (Caixa, Cozinha, Bar) ao mesmo agente
3. Cada terminal tem seu nomeImpressora configurado no backend
4. O agente imprime cada job na impressora informada automaticamente
```

### Cenário C — Computadores separados por setor

```
1. Instalar 1 agente em cada computador
2. Cada agente recebe um agent-bootstrap.json com chave diferente
3. No sistema web, vincular cada chave ao terminal correspondente
```

---

## 17. Cenários de Erro

| Cenário | Status | Mensagem de Log |
|---|---|---|
| `chaveAgente` vazia | CONFIGURACAO_PENDENTE | "Agente não configurado (chaveAgente ausente)" |
| `apiBaseUrl` vazia | — | "apiBaseUrl não configurada" |
| Backend HTTP ≠ 200/204 | Exceção → ERRO | "Erro HTTP ao buscar job: 5xx" |
| Impressora não encontrada | ERRO confirmado | "Impressora 'X' não encontrada no Windows" |
| Base64 inválido | ERRO confirmado | Mensagem da exception |
| Erro no spooler Windows | ERRO confirmado | "Falha ao imprimir na impressora 'X'" |
| Erro HTTP na confirmação | Log local | "Erro HTTP ao confirmar job: 5xx" |

---

## 18. Testes Obrigatórios

| Cenário | Verificação |
|---|---|
| Start com chaveAgente válida | Status = RODANDO, log mostra "Executando ciclo" |
| Start sem chaveAgente | Status = CONFIGURACAO_PENDENTE |
| Job com impressora existente | Log mostra "impresso com sucesso", confirmação OK |
| Job com impressora inexistente | Confirmação ERRO, log mostra nome da impressora ausente |
| Job sem nomeImpressora + fallback configurado | Imprime no fallback |
| Polling em pico (jobs em sequência) | Próximo polling = 1000ms |
| Polling ocioso (6+ ciclos sem job) | Próximo polling = 15000ms |
| Erro de conexão | Próximo polling = 15000ms, status = ERRO |
| Chave mascarada | `GET /api/agent/status` retorna chaveAgenteMascarada sem expor valor completo |
| Bootstrap de instalação | `agent-bootstrap.json` lido, chave salva, arquivo renomeado |

---

## 19. Compatibilidade e Migração

### 19.1 Migração da chave de acesso

**A migração de `chaveAcesso` antiga para `chaveAgente` não deve ser automática por padrão.**

`chaveAcesso` pertencia ao caixa no modelo antigo. `chaveAgente` representa uma entidade nova no backend (`AgenteImpressao`). Copiar a chave antiga automaticamente só é seguro se o backend já tiver migrado os registros de caixa para agentes de impressão, preservando a mesma chave.

Opções documentadas:

**Opção A — Migração controlada pelo backend:**
O backend migra os caixas ativos para agentes de impressão, preservando a `chaveAcesso` como `chaveAgente`. Após essa migração, o agente pode copiar automaticamente. Deve haver coordenação explícita entre equipe de backend e suporte.

**Opção B — Reinstalação recomendada (padrão seguro):**
```
Se chaveAgente não existir:
    status = CONFIGURACAO_PENDENTE
    mensagem = "Agente não ativado. Baixe novamente o agente pelo sistema."
```
O operador é orientado a baixar o novo instalador (com `agent-bootstrap.json`) pelo sistema web.

**Regra recomendada:** preferir a Opção B para novos deployments. A Opção A só é válida se o backend garantir compatibilidade das chaves.

### 19.2 Campos do modelo antigo

| Campo | Comportamento no novo agente |
|---|---|
| `chaveAcesso` | NÃO migrado automaticamente — ver regra acima |
| `idCaixa` | Ignorado no novo fluxo; permanece no SQLite sem causar erro |
| `intervaloMs` | Ignorado; polling agora é totalmente adaptativo |

### 19.3 Coexistência de endpoints legados e novos

Durante o rollout, o backend principal deve suportar **simultaneamente** os dois fluxos:

**Agentes legados (instalações antigas):**
```http
GET  /proximo?idCaixa={id}&chaveAcesso={chave}
POST /confirma
```

**Agentes novos (modelo chaveAgente):**
```http
GET  /agente/proximo        Header: X-Agente-Key
POST /agente/confirma       Header: X-Agente-Key
```

**Regra de implantação:**

Não remover os endpoints legados enquanto houver agentes antigos instalados em clientes. A remoção do fluxo legado só deve ocorrer após confirmação de migração completa de todos os pontos de instalação.

---

## 20. Arquivos Principais do Código

| Arquivo | Responsabilidade |
|---|---|
| `scheduler/ImpressaoScheduler.java` | Ciclo de polling adaptativo |
| `service/imp/OrquestradorImpressaoServiceImp.java` | Orquestra busca → impressão → confirmação |
| `client/ImpressaoApiClientBack.java` | HTTP para backend (endpoints `/agente/proximo` e `/agente/confirma`) |
| `printer/PrinterResolver.java` | Seleciona impressora Windows por nome com fallback em cascata |
| `printer/MotorImpressao.java` | Envia bytes para o spooler Windows via Java Print Service |
| `printer/PrintJobContext.java` | Contexto do job passado ao motor de impressão |
| `config/ConfiguracaoAgente.java` | Lê configuração do SQLite (chaveAgente, apiBaseUrl, etc.) |
| `db/AgentDatabaseInitializer.java` | Cria/migra banco SQLite + bootstrap de instalação |
| `service/imp/AgentStateServiceSQLite.java` | Estado do agente persistido no SQLite |
| `controller/AgentConfigController.java` | API de configuração (`GET/POST /api/agent/config`) |
| `controller/AgentController.java` | API de status e logs (`GET /api/agent/status`, `/api/agent/logs`) |
| `controller/PrinterController.java` | API de impressoras (`GET /api/agent/printer/status`) |

---

## Endpoints Locais

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/agent/status` | Estado atual do agente |
| GET | `/api/agent/logs?limite=N` | Últimos N logs |
| GET | `/api/agent/config` | Configuração atual |
| POST | `/api/agent/config` | Salva configuração |
| POST | `/api/agent/config/test-connection` | Testa conexão com backend |
| POST | `/api/agent/config/test-print` | Envia impressão de teste |
| GET | `/api/agent/printer/status` | Lista impressoras detectadas |

## Endpoints Remotos Consumidos

| Método | Endpoint | Headers |
|---|---|---|
| GET | `{apiBaseUrl}/agente/proximo` | `X-Agente-Key` |
| POST | `{apiBaseUrl}/agente/confirma` | `X-Agente-Key`, `Content-Type: application/json` |

---

## Pendências que dependem do backend principal

1. **Implementar** `GET /agente/proximo` com autenticação por `X-Agente-Key`.
2. **Implementar** `POST /agente/confirma` com `X-Agente-Key`.
3. **Criar** entidade `AgenteImpressao` com `chaveAgente` e lista de terminais vinculados.
4. **Adaptar** fila de impressão para retornar `terminalNome`, `nomeImpressora`, `larguraPapelMm`, `proximoPollingMs`.
5. **Gerar** `agent-bootstrap.json` ao cadastrar um novo agente no sistema web.
6. **Manter** endpoints antigos (`/proximo?idCaixa=X`) em paralelo durante a migração de agentes legados.
