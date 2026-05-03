# MAPEAMENTO COMPLETO — AGENTE DE IMPRESSÃO (PMEI)

**Data:** 2026-05-02
**Tipo:** Diagnóstico — somente leitura, nenhum código alterado.

---

## 1. Visão Geral do Fluxo Atual

O sistema é formado por dois componentes distintos:

- **Backend (externo):** responsável por gerar o documento, manter a fila e autenticar o agente.
- **Agente (este repositório):** Spring Boot local, porta 9090, responsável por consultar a fila e enviar bytes para a impressora Windows.

### Fluxo ponta a ponta

```
ImpressaoScheduler (@Scheduled, fixedDelay=1000ms)
  └─ aguarda intervaloMs configurável (default ~100s no SQLite)
  └─ OrquestradorImpressaoServiceImp.executarCiclo()
       └─ valida: idCaixa, chaveAcesso, apiBaseUrl configurados
       └─ ImpressaoApiClientBack.buscarProximoJob(idCaixa, chaveAcesso)
            GET {baseUrl}/proximo?idCaixa={id}&chaveAcesso={chave}
            ↓ resposta do backend (ProximoJobResponse)
       └─ Base64.decode(job.conteudo) → byte[]
       └─ MotorImpressao.printRawBytes(bytes, "POSMEI-" + tipoDocumento)
            └─ DetectorImpressora.detectarImpressoraPadrao()
                 → busca impressora com nome contendo "elgin"
                 → fallback: primeira impressora disponível no Windows
            └─ javax.print.PrintService.print()
                 → Windows Print Spooler → impressora física
       └─ ImpressaoApiClientBack.confirmarJob(idJob, "OK", null)
            POST {baseUrl}/confirma
            ↓ backend marca job como impresso
```

---

## 2. Arquitetura do Projeto

Este repositório contém **apenas o agente**, não o backend.

| Componente | Localização |
|---|---|
| Agente (Spring Boot 3.3.2, Java 21) | `src/main/java/zse/softease/agente_pmei/` |
| Frontend integrado (SPA) | `src/main/resources/static/` |
| Banco local SQLite | `agent.db` (na pasta de execução do jar) |
| Backend/API | Externo — não está neste repositório |

O agente expõe sua própria API REST na porta **9090** (localhost apenas) e consome o backend externo via HTTP.

---

## 3. Fluxo de Cupom

O agente não conhece "cupom" como entidade. Do ponto de vista do agente:

- O backend gera o ESC/POS do cupom.
- Coloca na fila com `idCaixa` e `tipoDocumento = "CUPOM"` (valor real a confirmar no backend).
- O agente busca o próximo job, decodifica Base64 e envia para a impressora.
- O `tipoDocumento` é usado apenas como nome do job no spooler do Windows.

**Fluxo resumido:**
```
Backend: gera ESC/POS do cupom → codifica em Base64 → grava em fila (idCaixa=X)
Agente:  GET /proximo?idCaixa=X → recebe Base64 → decode → print → POST /confirma OK
```

---

## 4. Fluxo de Pedido Operacional

Idêntico ao cupom sob a perspectiva do agente. O campo `tipoDocumento` diferencia o tipo (ex.: `"PEDIDO_OPERACIONAL"`), mas o processamento é o mesmo: decode Base64 → raw bytes → impressora.

O agente não possui lógica diferenciada por tipo de documento.

---

## 5. Fila de Impressão

**A fila de impressão NÃO está neste repositório.** Ela está no backend externo.

O agente apenas consulta e confirma. Os campos abaixo são inferidos pelo que o agente recebe/envia:

| Campo | Localização | Observação |
|---|---|---|
| `idJob` | `ProximoJobResponse.idJob` (Long) | Identificador único do job, retornado pelo backend |
| `tipoDocumento` | `ProximoJobResponse.tipoDocumento` (String) | Tipo do documento (cupom, pedido, etc.) |
| `conteudo` | `ProximoJobResponse.conteudo` (String) | Payload em Base64 (ESC/POS ou outro formato) |
| `nomeImpressora` | `ProximoJobResponse.nomeImpressora` (String, nullable) | Existe no response, mas **não é utilizado** pelo agente |
| `larguraPapelMm` | `ProximoJobResponse.larguraPapelMm` (Integer, nullable) | Existe no response, mas **não é utilizado** pelo agente |
| `idCaixa` | Parâmetro de query na requisição do agente | Filtro de roteamento da fila no backend |
| `chaveAcesso` | Parâmetro de query na requisição do agente | Autenticação do agente no backend |
| `idEmpresa` | **Não encontrado** | Não está nos DTOs do agente |
| `idUsuario` | **Não encontrado** | Não está nos DTOs do agente |
| `status` | **Não encontrado no agente** | Controlado pelo backend; agente apenas confirma OK ou ERRO |
| `dataCriacao` | **Não encontrado no agente** | Controlado pelo backend |
| `dataImpressao` | **Não encontrado no agente** | Controlado pelo backend após confirmação |
| `dataErro` | **Não encontrado no agente** | Controlado pelo backend |
| `tentativas` | **Não encontrado no agente** | Agente não controla tentativas |
| `mensagemErro` | `ConfirmarRequest.mensagemErro` (String) | Enviado ao backend somente em caso de falha |

---

## 6. Roteamento da Fila

| Pergunta | Resposta |
|---|---|
| Fila filtrada por `idEmpresa`? | Não (agente não envia `idEmpresa`) |
| Fila filtrada por `idCaixa`? | **Sim** — `idCaixa` é o único filtro de roteamento |
| Fila filtrada por `chaveAcesso`? | **Sim** — `chaveAcesso` serve como autenticação/validação |
| Fila filtrada por usuário? | Não |
| Fila filtrada por nome de impressora? | Não (campo existe no response, mas é ignorado) |
| Fila filtrada por tipo de documento? | Não — agente recebe qualquer tipo de documento |
| Existe conceito de terminal de impressão? | **Não** |
| Existe conceito de setor de impressão? | **Não** |
| Existe conceito de destino de impressão? | **Não** |

**Conclusão:** A fila é roteada exclusivamente por `idCaixa`. Cada agente = um caixa = uma impressora.

---

## 7. Autenticação do Agente

| Pergunta | Resposta |
|---|---|
| Usa chave de acesso? | **Sim** — `chaveAcesso` (String) |
| Usa `idCaixa`? | **Sim** — `idCaixa` (Long) |
| Usa token JWT? | Não |
| Usa usuário/senha? | Não |
| Usa API Key? | Não |
| Usa empresa? | Não |
| Onde fica configurado? | SQLite local (`agent_config`) + UI em `http://127.0.0.1:9090/config` |
| Onde é validado no backend? | Desconhecido — backend externo, fora deste repositório |

**Mecanismo:** A cada ciclo de polling, o agente envia `idCaixa` e `chaveAcesso` como query params no `GET /proximo`. Não há sessão, nem token renovável.

---

## 8. Configuração do Agente

| Pergunta | Resposta |
|---|---|
| Possui tela de configuração? | **Sim** — `http://127.0.0.1:9090/config` |
| Armazena `idCaixa`? | **Sim** — `agent_config` (SQLite) |
| Armazena `chaveAcesso`? | **Sim** — `agent_config` (SQLite) |
| Armazena URL da API? | **Sim** — `apiBaseUrl` em `agent_config` |
| Armazena nome da impressora? | **Não** — impressora é detectada automaticamente (Elgin ou primeira) |
| Lista impressoras disponíveis? | **Sim** — `GET /api/agent/printer/status` |
| Permite selecionar impressora? | **Não** — seleção é automática, sem configuração |
| Permite mais de uma impressora? | **Não** |
| Onde as configurações ficam salvas? | `agent.db` (SQLite), tabela `agent_config` |

**Valor default do `apiBaseUrl`:**
```
http://127.0.0.1:8080/posmei-api/api/posmei/impressao
```

**Intervalo default de polling:** 100.000ms (100 segundos) — configurável via UI.

---

## 9. Consulta de Pendências pelo Agente

| Pergunta | Resposta |
|---|---|
| Qual endpoint o agente chama? | `GET {baseUrl}/proximo` |
| Quais parâmetros envia? | `idCaixa` (Long), `chaveAcesso` (String) — query params |
| Qual body envia? | Nenhum |
| Qual response recebe? | `ApiResponseWrapper<ProximoJobResponse>` |
| Busca uma impressão por vez ou várias? | **Uma por vez** |
| Existe paginação? | Não |
| Existe polling interval? | **Sim** — configurável (`intervaloMs`) |
| Intervalo vem do backend ou do agente? | **Do agente** (salvo em `agent_config.intervaloMs`) |

**Timeout do HTTP client:**
- Conexão: 5 segundos
- Leitura: 10 segundos

---

## 10. Confirmação de Impressão

| Pergunta | Resposta |
|---|---|
| Qual endpoint marca como impresso? | `POST {baseUrl}/confirma` |
| Qual status é gravado? | `"OK"` (sucesso) ou `"ERRO"` (falha) |
| Existe `data_hora_impressao`? | Não registrado pelo agente |
| Existe agente responsável? | Não (identificação é apenas `idJob`) |
| Existe controle de sucesso? | Sim — `status: "OK"` |
| Existe controle de erro? | Sim — `status: "ERRO"` + `mensagemErro` |
| Existe retry? | **Não** — agente não reprocessa job com erro |

**Body enviado:**
```json
{
  "idJob": 123,
  "status": "OK",
  "mensagemErro": null
}
```

---

## 11. Tratamento de Erro

| Cenário | O que acontece |
|---|---|
| Impressora desligada | `PrintService.print()` lança exceção → agente envia `POST /confirma {status: "ERRO"}` → backend controla |
| Agente não está rodando | Jobs ficam pendentes no backend até o agente reiniciar e retomar polling |
| Falha ao imprimir | Status `"ERRO"` enviado ao backend com `mensagemErro` |
| Falha na requisição HTTP | Exceção capturada, logada localmente, ciclo encerrado — próximo ciclo tenta novamente |
| Fila reprocessa em erro? | Não gerenciado pelo agente — responsabilidade do backend |
| Limite de tentativas? | **Não existe no agente** |
| Tela para visualizar falhas? | **Sim** — `http://127.0.0.1:9090/logs` |

---

## 12. Payload de Impressão

| Pergunta | Resposta |
|---|---|
| Backend envia ESC/POS pronto? | **Sim** — inferido pelo uso de raw bytes + `larguraPapelMm` |
| Backend envia PDF? | Não (agente não processa PDF) |
| Backend envia HTML? | Não |
| Backend envia Base64? | **Sim** — `conteudo` é uma string Base64 |
| Agente monta comandos ESC/POS? | **Não** — apenas decodifica Base64 e envia para spooler |
| Payload tem nome da impressora? | Existe campo, mas **não é utilizado** |
| Payload tem largura do papel? | Existe campo, mas **não é utilizado** |
| Payload tem tipo de documento? | **Sim** — usado apenas como nome do job no spooler |

**Fluxo do payload:**
```
Backend → Base64(bytes ESC/POS) → ProximoJobResponse.conteudo
Agente  → Base64.decode()      → byte[]
Agente  → PrintService.print() → Windows Spooler → impressora
```

---

## 13. Impressora Local vs. Impressora da Fila

| Pergunta | Resposta |
|---|---|
| Impressora definida no caixa? | Não (caixa = `idCaixa` só serve para roteamento da fila) |
| Impressora definida no agente? | **Não** — sem configuração manual; detecção automática |
| Impressora enviada no payload? | Campo existe (`nomeImpressora`), mas é **ignorado** |
| Impressora buscada no banco? | **Não** |
| Quem decide para qual impressora imprimir? | **`DetectorImpressora`** — hardcoded: Elgin ou primeira disponível |

**Lógica atual de `DetectorImpressora`:**
```java
// Busca impressora cujo nome contenha "elgin" (case-insensitive)
// Se não encontrar, usa a primeira impressora listada pelo Java Print Service
```

---

## 14. Suporte a Múltiplas Impressoras

| Pergunta | Resposta |
|---|---|
| Agente suporta múltiplas impressoras? | **Não** |
| Agente escolhe impressora por nome? | **Não** — campo recebido é ignorado |
| Imprime em impressora de rede? | Depende do Windows — se a impressora estiver mapeada no SO, sim |
| Imprime em impressora compartilhada Windows? | Depende do mapeamento no SO |
| Limitações identificadas? | Seleção automática brittle (depende de nome "Elgin"); sem suporte a roteamento por impressora |

---

## 15. Relação com Caixa

| Pergunta | Resposta |
|---|---|
| Impressão sempre depende de caixa? | **Sim** — `idCaixa` é obrigatório para polling |
| Cupom depende de caixa? | Sim (pelo backend — inferido) |
| Pedido operacional depende de caixa? | Sim (pelo backend — inferido) |
| Etiqueta depende de caixa? | Desconhecido — backend controla |
| Agente depende de caixa? | **Sim** — `idCaixa` é pré-requisito de funcionamento |
| `chaveAcesso` está em `tb_caixa`? | Desconhecido — backend externo |
| Caixa define nome da impressora? | **Não** no agente; desconhecido no backend |

---

## 16. Relação com Usuário Logado

| Pergunta | Resposta |
|---|---|
| Backend usa usuário logado para descobrir caixa? | Desconhecido — backend externo |
| Como o caixa do usuário é obtido? | Desconhecido — backend externo |
| Pedido operacional usa caixa do usuário? | Desconhecido — backend externo |
| Cupom usa caixa do usuário? | Desconhecido — backend externo |
| Agente usa usuário logado ou apenas chave? | **Apenas `idCaixa` + `chaveAcesso`** — sem usuário |

---

## 17. Endpoints do Agente

### 17.1 Endpoints locais (expostos pelo agente)

| Método | Endpoint | Usado por | Retorno | Observação |
|---|---|---|---|---|
| GET | `/api/agent/status` | UI/dashboard | `AgentStateDTO` | Estado atual: RODANDO, PARADO, ERRO |
| GET | `/api/agent/logs?limite=N` | UI/logs | Lista de logs | Últimos N logs do SQLite |
| GET | `/api/agent/config` | UI/config | `AgentConfigDTO` | Configuração atual |
| POST | `/api/agent/config` | UI/config | Confirmação | Salva configuração no SQLite |
| POST | `/api/agent/config/test-connection` | UI/config | Resultado | Testa conexão com backend |
| POST | `/api/agent/config/test-print` | UI/config | Resultado | Envia teste de impressão |
| GET | `/api/agent/printer/status` | UI/config | Lista impressoras | Impressoras detectadas no Windows |

### 17.2 Endpoints do backend (consumidos pelo agente)

| Método | Endpoint | Parâmetros/Body | Retorno | Observação |
|---|---|---|---|---|
| GET | `{baseUrl}/proximo` | `idCaixa`, `chaveAcesso` (query) | `ProximoJobResponse` | Busca próximo job pendente |
| POST | `{baseUrl}/confirma` | `ConfirmarRequest` (body JSON) | Confirmação | Confirma impressão OK ou ERRO |

---

## 18. Classes e Métodos Principais

| Classe | Método | Responsabilidade | Fluxo |
|---|---|---|---|
| `ImpressaoScheduler` | `executar()` | Dispara ciclo a cada `intervaloMs` | Entrada do ciclo |
| `OrquestradorImpressaoServiceImp` | `executarCiclo()` | Orquestra busca, impressão e confirmação | Núcleo do agente |
| `ImpressaoApiClientBack` | `buscarProximoJob(idCaixa, chaveAcesso)` | GET ao backend | Comunicação com backend |
| `ImpressaoApiClientBack` | `confirmarJob(idJob, status, mensagemErro)` | POST ao backend | Confirmação de impressão |
| `MotorImpressao` | `printRawBytes(bytes, jobName)` | Envia bytes para Windows Print Service | Impressão física |
| `DetectorImpressora` | `detectarImpressoraPadrao()` | Detecta impressora Elgin ou primeira disponível | Seleção de impressora |
| `ConfigServiceSQLiteImp` | `get(chave)` / `set(chave, valor)` | Lê/escreve configurações no SQLite | Configuração persistente |
| `LogServiceSQLite` | `info(msg)` / `erro(msg, detalhe)` | Grava logs no SQLite | Logging local |
| `AgentStateServiceSQLite` | `marcarRodando()` / `marcarErro(msg)` | Atualiza estado do agente | Estado do agente |
| `AgentDatabaseInitializer` | `initialize()` | Cria tabelas SQLite na primeira execução | Setup do banco local |
| `AgentConfigController` | `getConfig()` / `saveConfig()` | Expõe e salva configuração via REST | API de configuração |
| `AgentController` | `getStatus()` / `getLogs()` | Expõe status e logs via REST | API de monitoramento |
| `PrinterController` | `getPrinterStatus()` | Lista impressoras do Windows via REST | API de diagnóstico |
| `TestPrintServiceImp` | `enviarTestePrint()` | Gera impressão de teste | Diagnóstico de impressora |

---

## 19. Tabelas do Banco Local (SQLite — agent.db)

| Tabela | Finalidade | Campos relevantes | Observação |
|---|---|---|---|
| `agent_config` | Configurações do agente | `chave TEXT PK`, `valor TEXT`, `atualizado_em` | Chaves: `idCaixa`, `chaveAcesso`, `apiBaseUrl`, `intervaloMs` |
| `agent_estado` | Estado atual do agente | `id`, `status TEXT`, `ultima_execucao`, `ultima_mensagem`, `atualizado_em` | Apenas 1 linha (id=1) |
| `agent_log` | Histórico de eventos | `id AUTOINCREMENT`, `data_hora`, `tipo TEXT`, `mensagem TEXT`, `detalhe TEXT` | Tipos: INFO, ERRO |
| `printer_status` | Impressoras detectadas | `id`, `nome TEXT`, `is_default`, `status`, `porta`, `driver`, `atualizado_em` | Preenchida pelo `PrinterController` |
| `agent_atividade` | Últimas atividades | `id`, `ultimo_job_recebido`, `ultimo_job_impresso`, `ultimo_erro`, `atualizado_em` | Apenas 1 linha (id=1) |

---

## 20. Enums e Tipos de Documento

| Item | Status |
|---|---|
| `EnumTipoDocumentoImpressao` | **Não existe no agente** |
| `EnumStatusImpressao` | **Não existe no agente** |
| Tipo de documento atual | String livre recebida do backend (`tipoDocumento`) |
| Valores esperados | `"CUPOM"`, `"PEDIDO_OPERACIONAL"` etc. — definidos no backend |
| Como adicionar novo tipo | No backend — o agente aceita qualquer string |

**Observação:** O agente é agnóstico ao tipo de documento. Não há switch/case ou lógica condicional por tipo.

---

## 21. Análise para Futura Impressão de Cozinha/Produção

| Pergunta | Resposta |
|---|---|
| Modelo atual suporta impressões por setor? | **Não** |
| Modelo atual suporta múltiplos agentes? | **Sim** — cada instância usa um `idCaixa` diferente |
| Modelo atual suporta um agente por cozinha/bar/chapa? | **Parcialmente** — seria um agente por setor, cada um com um `idCaixa` distinto |
| Modelo atual suporta um agente central com várias impressoras? | **Não** — sem roteamento por impressora |
| Modelo atual precisa ser evoluído? | **Sim** |

### Campos e Tabelas Ausentes (no backend — inferido)

- Campo `idSetor` ou `idTerminal` na tabela de fila
- Conceito de "terminal de impressão" dissociado de "caixa"
- Suporte a filtro da fila por setor/terminal

### Riscos de usar `idCaixa` para cozinha (sem evolução)

- Impressões de cozinha seriam associadas a caixas, misturando conceitos
- Configuração confusa: operador de cozinha teria que saber o `idCaixa` e `chaveAcesso`
- Cupons fiscais do caixa poderiam vazar para a impressora de cozinha
- Sem garantia de que a cozinha nunca imprima um cupom

---

## 22. Recomendações Técnicas

### Opção A — Um agente por ponto físico

```
Agente do caixa    → idCaixa = X → impressora do caixa
Agente da cozinha  → idCaixa = Y → impressora da cozinha
Agente do bar      → idCaixa = Z → impressora do bar
```

| | |
|---|---|
| Vantagens | Arquitetura simples; reusa tudo que existe; isolamento total entre setores |
| Desvantagens | Reusar `idCaixa` é semanticamente errado (cozinha não é um caixa); necessário criar "caixas fantasmas" para cada setor |
| Impacto no backend | Adicionar `idCaixa` para cada terminal de impressão ou criar entidade nova |
| Impacto no agente | Mínimo — apenas mudar `idCaixa` na configuração |
| Impacto na fila | Necessário que backend envie jobs à fila com o `idCaixa` correto por tipo de impressão |

### Opção B — Um agente central com várias impressoras

```
Um único agente → consulta fila por idTerminal → imprime em impressora X ou Y pelo nome
```

| | |
|---|---|
| Vantagens | Um único processo; centralizado; mais simples para o operador |
| Desvantagens | Agente precisa de lógica de roteamento por nome de impressora; campo `nomeImpressora` (hoje ignorado) teria que ser implementado |
| Impacto no backend | Enviar `nomeImpressora` na fila corretamente por tipo de impressão |
| Impacto no agente | Implementar roteamento por `nomeImpressora`; substituir `DetectorImpressora` pela lógica de seleção pelo nome |
| Impacto na fila | `nomeImpressora` já existe no DTO — basta ativá-lo |
| Limitações | Windows deve ter a impressora de rede mapeada com o nome exato; pode falhar silenciosamente se nome difere |

### Opção C — Modelo híbrido (recomendado)

```
Loja pequena  → 1 agente, 1 impressora para tudo (hoje já funciona)
Loja grande   → N agentes, cada agente = 1 terminal = 1 impressora
```

**Para suportar isso, a evolução necessária seria:**

1. **Backend:** Criar entidade `TerminalImpressao` (ou `PontoImpressao`) independente de `Caixa`.
2. **Backend:** Adaptar fila para ter `idTerminal` (não apenas `idCaixa`).
3. **Backend:** Na lógica de geração de impressão, decidir qual terminal recebe o job por tipo de documento.
4. **Agente:** Trocar `idCaixa` por `idTerminal` nos parâmetros de polling.
5. **Agente:** Implementar uso do campo `nomeImpressora` do payload (já existe, só está ignorado).
6. **Agente:** Tornar seleção de impressora configurável na UI (já lista via `GET /api/agent/printer/status`).

---

## 23. Proposta Conceitual para Cozinha/Produção

### Nome recomendado: **Terminal de Impressão**

Justificativa:
- "Ponto de Impressão" é mais genérico e pode ser confundido com endereço físico.
- "Terminal de Impressão" é explicitamente um dispositivo/instância que processa impressões.
- Aliado ao conceito existente de "Caixa", os termos ficam claros: um `TerminalImpressao` pode ser um caixa, uma cozinha, um bar, etc.

### Modelo proposto

```
TerminalImpressao {
  id
  idEmpresa
  nome              ("Caixa 1", "Cozinha", "Bar", "Chapa")
  nomeImpressora    (nome real da impressora no Windows)
  larguraPapelMm    (padrão para este terminal)
  chaveAcesso       (autenticação do agente)
  ativo
}
```

**Suporte ao Cenário A (loja pequena):**
- Um único `TerminalImpressao` com `nome = "Geral"`
- Backend envia todos os tipos de documento para o mesmo terminal

**Suporte ao Cenário B (loja com impressora por setor):**
- `TerminalImpressao { id=1, nome="Caixa", nomeImpressora="Elgin-Caixa" }`
- `TerminalImpressao { id=2, nome="Cozinha", nomeImpressora="Epson-Cozinha" }`
- `TerminalImpressao { id=3, nome="Bar", nomeImpressora="Bematech-Bar" }`
- Backend, ao criar impressão de cozinha, aponta `idTerminal = 2`
- Agente instalado na cozinha usa `idTerminal = 2` + `chaveAcesso`
- Backend filtra fila por `idTerminal`

---

## 24. Riscos se Cozinha For Implementada Sem Evoluir o Agente/Fila

| Risco | Severidade |
|---|---|
| Impressão sair no setor errado (cupom na cozinha, pedido no caixa) | Alta |
| Impressão duplicada se dois agentes consumirem o mesmo `idCaixa` | Alta |
| Pedido da cozinha imprimir antes da aceitação (se fila não tiver controle de estado) | Média |
| Cancelamento de item não chegar à impressora correta | Alta |
| Agente do caixa consumir impressão destinada à cozinha | Alta |
| Dificuldade de configurar múltiplas impressoras (sem UI de seleção) | Média |
| Dependência de computador local para cada ponto de impressão | Média |
| Falha silenciosa se nome da impressora não bater (campo hoje ignorado) | Alta |
| Ausência de retry no agente — job com erro fica preso no backend | Média |

---

## 25. Lacunas Identificadas

| Lacuna | Impacto |
|---|---|
| Campo `nomeImpressora` recebido do backend, mas ignorado pelo agente | Roteamento por impressora inviável hoje |
| Campo `larguraPapelMm` recebido, mas ignorado | Agente não adapta impressão ao papel |
| Sem retry automático no agente | Falhas de impressão podem exigir intervenção manual |
| Sem conceito de terminal/setor | Impressão de cozinha inviável sem evolução |
| Seleção de impressora hardcoded ("Elgin" ou primeira) | Frágil — qualquer renomeação quebra |
| `intervaloMs` default 100.000ms (100s) | Latência alta — jobs esperariam até 100s para serem processados |
| Sem persistência de tentativas no agente | Sem controle de quantas vezes um job foi tentado |

---

## 26. Lista de Arquivos Analisados

| Arquivo | Relevância |
|---|---|
| `src/main/java/.../scheduler/ImpressaoScheduler.java` | Ciclo de polling |
| `src/main/java/.../service/imp/OrquestradorImpressaoServiceImp.java` | Núcleo do agente |
| `src/main/java/.../client/ImpressaoApiClientBack.java` | Comunicação com backend |
| `src/main/java/.../printer/MotorImpressao.java` | Envio para impressora |
| `src/main/java/.../printer/DetectorImpressora.java` | Seleção de impressora |
| `src/main/java/.../dto/ProximoJobResponse.java` | Payload recebido do backend |
| `src/main/java/.../dto/ConfirmarRequest.java` | Payload de confirmação |
| `src/main/java/.../dto/AgentConfigDTO.java` | DTO de configuração |
| `src/main/java/.../dto/AgentStateDTO.java` | DTO de estado |
| `src/main/java/.../dto/ApiResponseWrapper.java` | Wrapper de resposta |
| `src/main/java/.../config/ConfiguracaoAgente.java` | Bean de configuração |
| `src/main/java/.../db/AgentDatabase.java` | Setup SQLite |
| `src/main/java/.../db/AgentDatabaseInitializer.java` | Criação das tabelas |
| `src/main/java/.../service/imp/ConfigServiceSQLiteImp.java` | CRUD de configuração |
| `src/main/java/.../service/imp/LogServiceSQLite.java` | Logging local |
| `src/main/java/.../service/imp/AgentStateServiceSQLite.java` | Estado do agente |
| `src/main/java/.../service/imp/TestPrintServiceImp.java` | Teste de impressão |
| `src/main/java/.../controller/AgentController.java` | API: status e logs |
| `src/main/java/.../controller/AgentConfigController.java` | API: configuração |
| `src/main/java/.../controller/PrinterController.java` | API: impressoras |
| `src/main/resources/application.properties` | Configurações Spring Boot |

---

## 27. Pontos Não Encontrados Neste Repositório

| Item | Observação |
|---|---|
| Código do backend | Repositório externo — não analisado aqui |
| Tabela de fila de impressão (`tb_fila_impressao` ou similar) | No backend externo |
| Enums de tipo de documento e status | No backend externo |
| Controller/Service de cupom | No backend externo |
| Controller/Service de pedido operacional | No backend externo |
| Migrations SQL do backend | No backend externo |
| Lógica de geração de ESC/POS | No backend externo |
| Validação de `chaveAcesso` | No backend externo |
| Conceito de `tb_caixa` | No backend externo |
| Suporte a múltiplas impressoras | Não existe em nenhum componente |

---

## Critérios de Conclusão — Respostas

| Critério | Resposta |
|---|---|
| Como o agente decide quais impressões imprimir? | Delega ao backend — filtra por `idCaixa` + `chaveAcesso`; backend devolve uma impressão por vez |
| Como a fila atual é roteada? | Por `idCaixa` — exclusivamente |
| Como o agente autentica? | `idCaixa` + `chaveAcesso` como query params no `GET /proximo` |
| Como o backend envia impressão para fila? | Desconhecido no agente; o agente apenas consome |
| Como o agente confirma impressão? | `POST {baseUrl}/confirma` com `{idJob, status, mensagemErro}` |
| O modelo atual suporta cozinha/bar/chapa? | **Não** — sem roteamento por setor |
| O que precisa mudar para suportar impressão por setor? | Criar entidade `TerminalImpressao` no backend; adaptar fila; implementar uso de `nomeImpressora` no agente |

---

*Arquivo gerado por diagnóstico — nenhum código foi alterado.*
