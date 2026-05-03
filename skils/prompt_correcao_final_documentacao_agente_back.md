# Prompt — Correção Final da Documentação do Agente Back

## Objetivo

Corrigir os pontos técnicos pendentes no documento do agente de impressão:

```text
agente-back-funcionamento.md
```

A documentação já está aprovada conceitualmente, mas precisa de ajustes pontuais antes de virar referência final para implementação.

Este ajuste é **documental**.

Não alterar código.  
Não alterar classes.  
Não alterar endpoints.  
Não alterar SQLite.  
Não alterar front.  
Apenas corrigir e complementar a documentação.

---

## Arquivo alvo

Atualizar:

```text
agente-back-funcionamento.md
```

---

# 1. Corrigir estratégia do scheduler

## Problema

O documento descreve o fluxo como:

```text
@Scheduled fixedDelay=100ms + Thread.sleep adaptativo
```

Essa abordagem funciona, mas não é a melhor para manter o agente estável. Usar `Thread.sleep` dentro de método agendado pode travar a thread do scheduler e dificultar manutenção.

## Ajuste esperado

Substituir a descrição por uma abordagem sem `Thread.sleep`.

Modelo recomendado:

```text
@Scheduled(fixedDelay = 1000)
  └─ verifica se agora >= proximaExecucao
       ├─ se não: retorna sem executar ciclo
       └─ se sim: executa ciclo
              └─ calcula proximoPollingMs
              └─ atualiza proximaExecucao = agora + proximoPollingMs
```

Explicar que o polling continua adaptativo, mas o agendamento é controlado por `proximaExecucao`.

## Texto sugerido

```md
O `ImpressaoScheduler` não deve usar `Thread.sleep` dentro do método agendado.

A estratégia recomendada é executar o scheduler com um `fixedDelay` curto, por exemplo 1000ms, e manter em memória/estado local a próxima data/hora permitida para consulta.

Fluxo:

1. Scheduler acorda.
2. Verifica `now >= proximaExecucao`.
3. Se ainda não chegou a hora, retorna.
4. Se chegou, executa o ciclo.
5. Ao final, calcula o próximo intervalo adaptativo.
6. Atualiza `proximaExecucao`.

Com isso, o agente mantém polling inteligente sem bloquear a thread do scheduler.
```

---

# 2. Ajustar regra de migração de `chaveAcesso` para `chaveAgente`

## Problema

O documento afirma que, se existir `chaveAcesso` antiga e `chaveAgente` estiver vazia, o agente copia automaticamente:

```text
chaveAcesso → chaveAgente
```

Isso pode ser perigoso.

No modelo novo, `chaveAgente` representa uma entidade nova no backend principal (`AgenteImpressao`). Já a `chaveAcesso` antiga pertence ao caixa.

Se o backend não tiver migrado as chaves antigas para agentes, essa cópia automática pode gerar erro de autenticação.

## Ajuste esperado

Trocar a regra automática por regra controlada.

Opções documentadas:

## Opção A — Migração controlada pelo backend

```text
Usar chaveAcesso antiga como chaveAgente somente se o backend tiver migrado os caixas atuais para agentes de impressão e mantido a mesma chave.
```

## Opção B — Reinstalação recomendada

```text
Se chaveAgente não existir:
    status = CONFIGURACAO_PENDENTE
    mensagem = "Agente não ativado. Baixe novamente o agente pelo sistema."
```

## Regra recomendada para documentação

```text
Não copiar chaveAcesso para chaveAgente automaticamente sem garantia de compatibilidade no backend.
```

## Texto sugerido

```md
A migração de `chaveAcesso` antiga para `chaveAgente` não deve ser automática por padrão.

Ela só pode ocorrer se o backend principal já tiver migrado os registros antigos de caixa para agentes de impressão, preservando a chave como chave válida de agente.

Caso contrário, o agente novo deve entrar em `CONFIGURACAO_PENDENTE` e orientar a reinstalação/reativação pelo sistema web.
```

---

# 3. Destacar manutenção dos endpoints legados

## Problema

O documento informa corretamente que o agente novo não chama mais:

```http
GET /proximo?idCaixa={idCaixa}&chaveAcesso={chave}
```

Mas é importante reforçar que o backend principal deve manter os endpoints antigos enquanto existirem agentes legados em produção.

## Ajuste esperado

Adicionar uma seção ou observação em Compatibilidade e Migração:

```md
Durante a transição, o backend principal deve manter em paralelo:

- fluxo legado por `idCaixa + chaveAcesso`;
- fluxo novo por `X-Agente-Key`.

Os endpoints legados só devem ser removidos depois que todos os agentes instalados forem atualizados/migrados.
```

## Texto sugerido

```md
Regra de implantação:

Não remover os endpoints legados enquanto houver agentes antigos instalados em clientes.

O backend deve suportar simultaneamente:

1. Agentes legados:
   - `GET /proximo?idCaixa=...&chaveAcesso=...`
   - `POST /confirma`

2. Agentes novos:
   - `GET /agente/proximo`
   - `POST /agente/confirma`
   - header `X-Agente-Key`

A remoção do fluxo legado só deve ocorrer após confirmação de migração completa.
```

---

# 4. Registrar risco de confirmação pendente local

## Problema

O documento diz que, se houver erro HTTP ao confirmar o job, o erro é apenas logado localmente e o job pode ficar pendente no backend.

Esse é um risco importante: a impressão pode ter saído fisicamente, mas o backend não recebe a confirmação. Isso pode gerar reimpressão indevida.

## Ajuste esperado

Documentar esse ponto como risco e pendência técnica.

Criar seção:

```md
## Confirmações pendentes locais
```

Explicar o risco e a evolução recomendada.

## Texto sugerido

```md
### Confirmação pendente local — pendência técnica

Se o agente imprimir fisicamente o job, mas falhar ao confirmar no backend por erro HTTP, o backend pode manter o job como pendente.

Esse cenário pode gerar duplicidade de impressão se o mesmo job for entregue novamente ao agente.

Evolução recomendada:

Criar uma tabela local no SQLite, por exemplo:

`agent_confirmacao_pendente`

Campos sugeridos:

- id
- idJob
- status
- mensagemErro
- nomeImpressoraUsada
- dataHoraLocal
- tentativas
- criadoEm
- atualizadoEm

Fluxo sugerido:

1. Se a confirmação HTTP falhar, salvar confirmação pendente local.
2. Antes de buscar novo job, tentar reenviar confirmações pendentes.
3. Se reenviar com sucesso, remover ou marcar como resolvida.
4. Registrar logs claros.
```

Classificar como:

```text
Risco: médio/alto
Impacto: possível duplicidade de impressão
Status: pendência técnica recomendada para próxima evolução
```

---

# 5. Conferir visão técnica do front local

O documento já informa que o front local do agente será um painel técnico.

Conferir se a seção `Front Local Técnico` deixa claro que:

```text
- o front local não é para usuário comum;
- mostra status do agente;
- mostra última consulta e próxima consulta;
- mostra último job;
- mostra última impressora usada;
- lista impressoras detectadas;
- em modo técnico permite editar API Base URL;
- em modo técnico mostra chave mascarada;
- em modo técnico permite configurar impressora fallback;
- em modo técnico permite testar conexão;
- em modo técnico permite testar impressão;
- não expõe polling manual para cliente comum;
- não expõe idCaixa no novo fluxo.
```

Se algum ponto não estiver claro, complementar a seção.

---

# 6. Não fazer

Não fazer:

```text
- Não alterar código.
- Não alterar endpoint real.
- Não alterar DTO real.
- Não alterar SQLite real.
- Não alterar front.
- Não remover compatibilidade antiga da documentação.
- Não afirmar que chaveAcesso antiga sempre vira chaveAgente.
- Não recomendar Thread.sleep no scheduler.
```

---

# 7. Checklist final

Após o ajuste, validar:

```text
[ ] Documento não recomenda mais Thread.sleep dentro do scheduler.
[ ] Documento descreve scheduler com proximaExecucao.
[ ] Documento trata migração chaveAcesso → chaveAgente como controlada/opcional.
[ ] Documento orienta CONFIGURACAO_PENDENTE quando não houver chaveAgente válida.
[ ] Documento reforça manutenção dos endpoints legados durante rollout.
[ ] Documento registra risco de confirmação pendente local.
[ ] Documento propõe agent_confirmacao_pendente como evolução.
[ ] Documento mantém o modelo por chaveAgente + terminais.
[ ] Documento mantém o polling adaptativo com teto de 15s.
[ ] Documento deixa claro que front local é técnico.
```

---

# 8. Entrega esperada

Ao final, informar:

```text
- agente-back-funcionamento.md atualizado.
- Scheduler documentado sem Thread.sleep.
- Migração de chave antiga documentada como controlada.
- Compatibilidade com endpoints legados reforçada.
- Risco de confirmação pendente documentado.
- Front local técnico revisado na documentação.
- Nenhum código alterado.
```
