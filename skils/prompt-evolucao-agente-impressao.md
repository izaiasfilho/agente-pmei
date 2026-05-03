# PROMPT — EVOLUÇÃO DO AGENTE DE IMPRESSÃO

## Instrução inicial obrigatória

Antes de qualquer alteração, rode o **Módulo 0** conforme o padrão do projeto.

O Módulo 0 deve:

1. Ler o `CLAUDE.md` ou arquivo principal de instruções do projeto.
2. Ler o diagnóstico já criado do agente, principalmente:
   - `/skils/impressao/agente-impressao-mapeamento.md`
3. Confirmar a estrutura real do agente:
   - Spring Boot local
   - SQLite local (`agent.db`)
   - UI local em `http://127.0.0.1:9090`
   - polling em `{apiBaseUrl}/proximo`
   - confirmação em `{apiBaseUrl}/confirma`
4. Identificar os arquivos reais antes de alterar.
5. Garantir compatibilidade com o fluxo atual por `idCaixa + chaveAcesso`.
6. Não quebrar cupom, pedido operacional ou qualquer fluxo já em produção.

---

# 1. Objetivo

Evoluir o agente de impressão para corrigir limitações existentes e prepará-lo para a futura arquitetura de impressão por **Terminal de Impressão**, **Setor de Preparo** e **Impressão de Cozinha/Produção**.

Hoje o agente possui limitações importantes:

1. Ignora `nomeImpressora` recebido do backend.
2. Ignora `larguraPapelMm` recebido do backend.
3. Detecta impressora automaticamente buscando nome contendo `"elgin"` ou usando a primeira impressora disponível.
4. Pode se perder quando há várias impressoras instaladas no Windows.
5. Usa `idCaixa + chaveAcesso` como identificação.
6. Usa SQLite local com pré-configurações.
7. Usa URL local como default:
   ```text
   http://127.0.0.1:8080/posmei-api/api/posmei/impressao
   ```
8. Não suporta terminal de impressão.
9. Não suporta seleção manual da impressora.
10. Não suporta múltiplas impressoras de forma controlada.

O objetivo deste ajuste é evoluir o agente com segurança, mantendo compatibilidade com o modelo atual.

---

# 2. Premissas obrigatórias

## 2.1 Compatibilidade com o legado

O agente atual funciona com:

```text
idCaixa
chaveAcesso
apiBaseUrl
intervaloMs
```

Essas chaves existem no SQLite local em `agent_config`.

Não remover essas chaves agora.

O agente deve continuar funcionando no modo atual:

```http
GET {apiBaseUrl}/proximo?idCaixa={idCaixa}&chaveAcesso={chaveAcesso}
```

e:

```http
POST {apiBaseUrl}/confirma
```

---

## 2.2 Preparação para modelo novo

Adicionar suporte futuro a:

```text
idTerminalImpressao
```

ou nome equivalente adotado no projeto.

Mas sem quebrar o modo atual.

A regra deve ser:

```text
Se idTerminalImpressao estiver configurado:
    usar modo novo por terminal
Senão:
    usar modo legado por idCaixa
```

O endpoint novo definitivo depende do backend.

Portanto, implementar de forma configurável e compatível.

---

# 3. Evolução da configuração SQLite

## 3.1 Chaves atuais

Manter as chaves atuais:

```text
idCaixa
chaveAcesso
apiBaseUrl
intervaloMs
```

## 3.2 Novas chaves sugeridas

Adicionar suporte às seguintes chaves em `agent_config`:

```text
idTerminalImpressao
modoIdentificacao
nomeImpressoraPadrao
usarNomeImpressoraDoJob
larguraPapelPadraoMm
```

## 3.3 Descrição das chaves

### `idTerminalImpressao`

Identificador do terminal de impressão no backend.

Usado futuramente para substituir `idCaixa`.

### `modoIdentificacao`

Valores:

```text
CAIXA
TERMINAL
```

Regra:

```text
CAIXA    = usar idCaixa + chaveAcesso
TERMINAL = usar idTerminalImpressao + chaveAcesso
```

Valor padrão inicial:

```text
CAIXA
```

### `nomeImpressoraPadrao`

Nome da impressora selecionada localmente na UI do agente.

Deve ser uma impressora listada pelo Windows.

### `usarNomeImpressoraDoJob`

Valores:

```text
true
false
```

Regra:

```text
true  = se o backend enviar nomeImpressora, tentar usar ela primeiro
false = ignorar nomeImpressora do job e usar nomeImpressoraPadrao
```

Valor padrão sugerido:

```text
true
```

### `larguraPapelPadraoMm`

Valores comuns:

```text
58
80
```

Valor padrão sugerido:

```text
58
```

---

# 4. Migração/Inicialização segura do SQLite

O agente usa SQLite local (`agent.db`).

Ajustar o inicializador do banco local para criar as novas chaves somente se não existirem.

Regras obrigatórias:

1. Não apagar o `agent.db`.
2. Não resetar configurações existentes.
3. Não sobrescrever `idCaixa`, `chaveAcesso`, `apiBaseUrl` ou `intervaloMs`.
4. Criar novas chaves com valores padrão apenas quando ausentes.
5. Garantir que instalações antigas sejam atualizadas automaticamente ao iniciar o agente novo.

Exemplo conceitual:

```text
se chave não existe:
    inserir valor padrão
senão:
    manter valor atual
```

---

# 5. Ajuste obrigatório da URL padrão

Hoje o default é local:

```text
http://127.0.0.1:8080/posmei-api/api/posmei/impressao
```

Isso não deve ser o padrão em produção.

Alterar o default para usar a URL oficial de produção do projeto.

## Regra obrigatória

1. Procurar nos arquivos do projeto, documentação, `.env`, `application.properties`, README ou instruções existentes qual é a URL oficial de produção.
2. Se encontrar a URL oficial, usar como default.
3. Se não encontrar, deixar configurável por propriedade de ambiente, por exemplo:
   ```properties
   agente.api-base-url-default=${AGENTE_API_BASE_URL:https://DOMINIO_PRODUCAO/posmei-api/api/posmei/impressao}
   ```
4. Não deixar `127.0.0.1` como default para distribuição/produção.
5. Usar como URL padrão oficial de produção:

   ```text
   https://api.zseposmei.cloud/posmei-api/api/posmei/impressao
   ```

6. `127.0.0.1` pode existir apenas como exemplo de desenvolvimento local na documentação.

## Documentação obrigatória

Documentar claramente:

```text
Desenvolvimento local:
http://127.0.0.1:8080/posmei-api/api/posmei/impressao

Produção:
https://api.zseposmei.cloud/posmei-api/api/posmei/impressao
```

---

# 6. Seleção manual de impressora

Hoje o agente lista impressoras via:

```http
GET /api/agent/printer/status
```

Mas não permite selecionar uma impressora.

Ajustar a UI local de configuração para permitir:

```text
Selecionar impressora padrão
Salvar nome da impressora em nomeImpressoraPadrao
Testar impressão nessa impressora
```

## Regra

A UI deve listar as impressoras detectadas no Windows e permitir selecionar uma.

Ao salvar, gravar no SQLite:

```text
nomeImpressoraPadrao
```

---

# 7. Nova lógica de seleção de impressora

Substituir a lógica frágil atual:

```text
procurar "elgin"
senão primeira impressora
```

por uma lógica controlada.

## Ordem de prioridade recomendada

Ao imprimir um job:

```text
1. Se usarNomeImpressoraDoJob = true e job.nomeImpressora estiver preenchido:
      tentar imprimir em job.nomeImpressora

2. Se não encontrar ou estiver vazio:
      tentar imprimir em nomeImpressoraPadrao configurada no agente

3. Se nomeImpressoraPadrao não estiver configurada:
      usar impressora padrão do Windows, se existir

4. Se ainda não encontrar:
      usar fallback legado: procurar "elgin"

5. Se ainda não encontrar:
      retornar erro controlado e confirmar job como ERRO
```

## Importante

Se `job.nomeImpressora` vier preenchido e não for encontrada, registrar log claro:

```text
Impressora informada pelo backend não encontrada: {nome}
```

Depois seguir fallback para impressora padrão local.

---

# 8. Uso do campo `nomeImpressora` recebido do backend

O DTO `ProximoJobResponse` já possui:

```text
nomeImpressora
```

Hoje ele é ignorado.

Ajustar o agente para utilizar esse campo conforme a regra de prioridade.

Isso resolve um problema atual: quando existem várias impressoras no Windows, o agente pode imprimir na impressora errada.

---

# 9. Uso do campo `larguraPapelMm`

O DTO `ProximoJobResponse` já possui:

```text
larguraPapelMm
```

Hoje ele é ignorado.

Como o backend já envia ESC/POS pronto em Base64, o agente provavelmente não precisa recalcular layout.

Mas o agente deve:

1. Armazenar o campo para log/diagnóstico.
2. Exibir no log do job.
3. Usar como metadado caso exista lógica futura.
4. Se houver rotina de teste de impressão gerada pelo agente, usar a largura configurada para montar o teste.

Não alterar o ESC/POS vindo do backend.

---

# 10. Evolução do polling

## Modo legado

Manter:

```http
GET {apiBaseUrl}/proximo?idCaixa={idCaixa}&chaveAcesso={chaveAcesso}
```

## Modo terminal

Preparar suporte para:

```http
GET {apiBaseUrl}/proximo?idTerminalImpressao={idTerminalImpressao}&chaveAcesso={chaveAcesso}
```

ou endpoint equivalente definido pelo backend.

## Regra

A URL e os parâmetros devem ser montados por estratégia:

```text
modoIdentificacao = CAIXA
    usa idCaixa

modoIdentificacao = TERMINAL
    usa idTerminalImpressao
```

## Compatibilidade

Enquanto o backend novo não estiver disponível, o modo padrão deve continuar sendo:

```text
CAIXA
```

---

# 11. Tela de configuração do agente

Atualizar a tela local:

```text
http://127.0.0.1:9090/config
```

Campos esperados:

```text
Modo de identificação: Caixa / Terminal de Impressão
ID do Caixa
ID do Terminal de Impressão
Chave de Acesso
URL da API
Intervalo de consulta
Impressora padrão
Usar impressora enviada pelo backend
Largura padrão do papel
```

## Regras da tela

Se modo = `CAIXA`:

```text
Exibir e obrigar ID do Caixa.
ID do Terminal pode ficar oculto ou opcional.
```

Se modo = `TERMINAL`:

```text
Exibir e obrigar ID do Terminal.
ID do Caixa pode ficar oculto ou opcional.
```

A chave de acesso continua obrigatória.

---

# 12. Teste de conexão

Atualizar o teste de conexão para respeitar:

```text
modoIdentificacao
idCaixa
idTerminalImpressao
chaveAcesso
apiBaseUrl
```

Se o backend ainda não suportar `idTerminalImpressao`, exibir erro amigável.

---

# 13. Teste de impressão

Atualizar o teste de impressão para permitir testar:

```text
impressora selecionada
largura padrão do papel
```

O teste deve imprimir claramente:

```text
TESTE DE IMPRESSÃO POSMEI
Impressora: {nomeImpressora}
Largura: {larguraPapelPadraoMm}mm
Data/Hora: ...
```

---

# 14. Logs e diagnóstico

Melhorar logs para registrar:

```text
idJob
tipoDocumento
nomeImpressora recebida do backend
nomeImpressora usada
larguraPapelMm recebida
modoIdentificacao
idCaixa ou idTerminalImpressao
resultado da impressão
mensagem de erro
```

Não logar chave de acesso.

---

# 15. Tratamento de erro

Se não encontrar impressora:

```text
Confirmar job como ERRO
Mensagem: Nenhuma impressora disponível ou configurada foi encontrada.
```

Se a impressora do backend não for encontrada, mas fallback imprimir com sucesso:

```text
Confirmar job como OK
Registrar aviso no log
```

Se a impressão falhar:

```text
Confirmar job como ERRO
Enviar mensagemErro clara para o backend
```

---

# 16. Documentação obrigatória

Atualizar ou criar:

```text
/skils/impressao/agente-evolucao-terminal-impressao.md
```

A documentação deve conter:

1. Problema atual.
2. Como o agente selecionava impressora antes.
3. Nova lógica de prioridade.
4. Novas chaves do SQLite.
5. Como atualizar instalações antigas.
6. Como configurar URL de produção.
7. Como configurar impressora padrão.
8. Como usar `nomeImpressora` recebido do backend.
9. Como funciona modo legado por caixa.
10. Como funcionará modo novo por terminal.
11. Como testar conexão.
12. Como testar impressão.
13. Riscos e rollback.

---

# 17. Restrições

Não remover suporte ao `idCaixa`.

Não quebrar o endpoint legado `/proximo?idCaixa=...`.

Não apagar SQLite.

Não sobrescrever configurações existentes.

Não deixar URL local como default de produção.

A URL padrão de produção deve ser:

```text
https://api.zseposmei.cloud/posmei-api/api/posmei/impressao
```

Não usar seleção hardcoded por `"elgin"` como regra principal.

Não ignorar `nomeImpressora` recebido do backend.

Não ignorar completamente `larguraPapelMm`; no mínimo registrar e exibir em logs.

Não logar chave de acesso.

---

# 18. Cenários de teste obrigatórios

## Cenário 1 — Instalação antiga

Configuração existente:

```text
idCaixa preenchido
chaveAcesso preenchida
apiBaseUrl preenchida
sem idTerminalImpressao
sem nomeImpressoraPadrao
```

Resultado esperado:

```text
Agente continua funcionando no modo CAIXA.
Nenhuma configuração antiga é perdida.
```

---

## Cenário 2 — Impressora padrão configurada

Configuração:

```text
nomeImpressoraPadrao = ELGIN-CAIXA
usarNomeImpressoraDoJob = false
```

Resultado esperado:

```text
Agente imprime sempre na impressora configurada, ignorando nomeImpressora do job.
```

---

## Cenário 3 — Nome da impressora vindo do backend

Configuração:

```text
usarNomeImpressoraDoJob = true
job.nomeImpressora = EPSON-COZINHA
```

Resultado esperado:

```text
Agente imprime na EPSON-COZINHA se existir no Windows.
```

---

## Cenário 4 — Impressora do backend não encontrada

Configuração:

```text
usarNomeImpressoraDoJob = true
job.nomeImpressora = IMPRESSORA-INEXISTENTE
nomeImpressoraPadrao = ELGIN-CAIXA
```

Resultado esperado:

```text
Agente registra aviso e imprime na ELGIN-CAIXA.
```

---

## Cenário 5 — Nenhuma impressora encontrada

Resultado esperado:

```text
Agente confirma job como ERRO.
Mensagem clara é enviada ao backend.
```

---

## Cenário 6 — URL de produção

Resultado esperado:

```text
Nova instalação não vem apontando para 127.0.0.1 como API principal de produção.
```

---

## Cenário 7 — Modo terminal

Configuração:

```text
modoIdentificacao = TERMINAL
idTerminalImpressao preenchido
chaveAcesso preenchida
```

Resultado esperado:

```text
Agente monta requisição usando idTerminalImpressao.
Se backend ainda não suportar, erro deve ser claro e controlado.
```

---

# 19. Resultado esperado

Ao final, o agente deve:

1. Continuar funcionando com `idCaixa + chaveAcesso`.
2. Permitir configurar impressora padrão.
3. Respeitar `nomeImpressora` enviado pelo backend quando habilitado.
4. Registrar `larguraPapelMm` em logs e diagnósticos.
5. Parar de depender da busca hardcoded por `"elgin"` como regra principal.
6. Usar URL de produção como default adequado ou variável de ambiente obrigatória.
7. Manter SQLite compatível com instalações antigas.
8. Estar preparado para futuro `idTerminalImpressao`.
9. Ter documentação atualizada.
