# PROMPT — MAPEAMENTO COMPLETO DO AGENTE DE IMPRESSÃO

## Instrução inicial obrigatória

Antes de qualquer análise ou alteração, rode o **Módulo 0** conforme o padrão do projeto.

O Módulo 0 deve ser executado para:

1. Ler o `CLAUDE.md` ou arquivo principal de instruções do projeto.
2. Identificar os padrões atuais de arquitetura.
3. Verificar a estrutura real do backend.
4. Verificar a estrutura real do agente de impressão.
5. Verificar os padrões atuais de:
   - Controller
   - Service
   - Service Imp
   - Repository
   - Entity
   - DTO
   - Enum
   - Fila de impressão
   - Autenticação do agente
   - Configuração de caixa/impressora
   - Documentação em `/skils`
6. Não alterar código nesta etapa.
7. Apenas mapear, analisar e documentar.

Esta tarefa é de **diagnóstico e documentação**.

Não implementar nenhuma alteração funcional agora.

---

# 1. Objetivo

Mapear completamente como funciona o **agente de impressão atual** do sistema.

A aplicação é web, então o backend não enxerga diretamente as impressoras locais.

Hoje o backend gera a impressão, envia para uma fila, e o agente instalado localmente consulta essa fila para imprimir.

O objetivo deste mapeamento é entender exatamente como esse fluxo funciona hoje antes de planejar a futura impressão para cozinha, bar, chapa ou outros setores de preparo.

---

# 2. Contexto do problema futuro

Será criada futuramente uma lógica de impressão para cozinha/produção.

Existem dois cenários que precisam ser suportados no futuro:

## Cenário A — Estabelecimento pequeno

O estabelecimento possui uma única impressora para tudo:

```text
cupom
pedido operacional
cozinha
bar
chapa
cancelamento de item
```

Nesse caso, todas as impressões podem sair no mesmo ponto físico.

---

## Cenário B — Estabelecimento com impressora por setor

O estabelecimento possui uma impressora para cada ponto de preparo:

```text
caixa/balcão
cozinha
bar
chapa
pizzaria
copa
```

Nesse caso, o agente/fila precisa garantir que uma impressão da cozinha não saia no caixa, e que uma impressão do bar não saia na cozinha.

---

# 3. Pergunta central do mapeamento

A pergunta principal que este diagnóstico deve responder é:

```text
Como o agente atual sabe quais impressões ele deve imprimir?
```

Também deve responder:

```text
A fila atual é roteada por caixa, por impressora, por usuário, por empresa ou por algum outro identificador?
```

---

# 4. Escopo da análise

Analisar o backend, o agente de impressão e as documentações existentes.

Procurar por termos como:

```text
impressao
impressão
fila
agente
caixa
chaveAcesso
chave_acesso
idCaixa
id_caixa
nomeImpressora
nome_impressora
larguraPapel
largura_papel
ESC/POS
escpos
CupomLayout
PedidoImpressao
EnviarParaImpressora
GerarImpressao
prepararCupomEnviarFilaImpressao
```

Ajustar os termos conforme os nomes reais encontrados no projeto.

---

# 5. Arquivos e áreas que devem ser investigados

Investigar todos os arquivos relacionados, especialmente:

```text
controller/
service/
service/imp/
repository/
repository/view/
entity/
entity/dto/
entity/enun/
config/
resources/db/migration/
```

Também investigar a pasta ou projeto do agente, caso exista separadamente.

Procurar por arquivos como:

```text
Agente
PrinterAgent
PrintAgent
ImpressaoAgent
ImpressaoController
ImpressaoService
ImpressaoImp
FilaImpressao
Caixa
CupomLayout
PedidoImpressaoLayout
EnviarParaImpressoraService
GerarImpressaoService
```

Não assumir nomes. Identificar os nomes reais no código.

---

# 6. Itens obrigatórios do mapeamento

O MD final deve responder claramente aos itens abaixo.

---

## 6.1 Visão geral do fluxo atual

Documentar o fluxo ponta a ponta:

```text
Front ou backend solicita impressão
→ backend monta documento
→ backend gera payload
→ backend envia para fila
→ agente consulta fila
→ agente imprime
→ agente atualiza status
```

Informar os nomes reais das classes, métodos, endpoints e tabelas envolvidos.

---

## 6.2 Tipos de impressão existentes

Identificar todos os tipos de impressão/documento existentes hoje.

Exemplos esperados:

```text
cupom
PDF de cupom
QR Code de cupom
pedido operacional
etiquetas
outros
```

Para cada tipo, documentar:

```text
endpoint
service
layout
tipo de documento
fila usada
payload enviado
se usa ESC/POS, PDF, Base64 ou outro formato
```

---

## 6.3 Impressão de cupom

Mapear especificamente o fluxo de cupom:

```text
endpoint de imprimir cupom
controller
service
repository/view
view SQL utilizada, se existir
layout utilizado
como gera ESC/POS
como envia para fila
qual tipo de documento é gravado
como o agente busca
como o agente confirma impressão
```

---

## 6.4 Impressão de pedido operacional

Mapear especificamente o fluxo de pedido operacional:

```text
endpoint de imprimir pedido
controller
service
layout utilizado
DTO utilizado
tipo de documento
como envia para fila
se compartilha infraestrutura com cupom
```

Responder:

```text
Pedido operacional usa a mesma fila do cupom?
Pedido operacional usa o mesmo método de envio para fila?
Pedido operacional depende de idCaixa?
```

---

## 6.5 Fila de impressão

Identificar a tabela ou estrutura da fila de impressão.

Documentar:

```text
nome da tabela
campos
relacionamentos
status possíveis
tipo de documento
idEmpresa
idCaixa
idUsuario
nomeImpressora
payload
base64
data criação
data impressão
data erro
mensagem erro
tentativas
```

Se algum campo não existir, documentar como:

```text
Não encontrado no código atual.
```

---

## 6.6 Roteamento da fila

Responder obrigatoriamente:

```text
A fila é filtrada por idEmpresa?
A fila é filtrada por idCaixa?
A fila é filtrada por chave de acesso?
A fila é filtrada por usuário?
A fila é filtrada por nome de impressora?
A fila é filtrada por tipo de documento?
Existe conceito de terminal de impressão?
Existe conceito de setor de impressão?
Existe conceito de destino de impressão?
```

---

## 6.7 Autenticação do agente

Mapear como o agente autentica no backend.

Responder:

```text
O agente usa chave de acesso?
O agente usa idCaixa?
O agente usa token?
O agente usa usuário/senha?
O agente usa API Key?
O agente usa empresa?
Onde isso é configurado?
Onde isso é validado no backend?
```

Documentar os endpoints reais.

---

## 6.8 Configuração do agente

Mapear como o agente é configurado localmente.

Responder:

```text
O agente possui tela de configuração?
O agente armazena idCaixa?
O agente armazena chave de acesso?
O agente armazena URL da API?
O agente armazena nome da impressora local?
O agente lista impressoras disponíveis?
O agente permite selecionar impressora?
O agente permite mais de uma impressora?
Onde as configurações ficam salvas?
```

Se o agente não estiver no repositório, documentar essa limitação.

---

## 6.9 Consulta de pendências pelo agente

Mapear o endpoint que o agente usa para consultar impressões pendentes.

Responder:

```text
Qual endpoint o agente chama?
Qual método HTTP?
Quais parâmetros envia?
Qual body envia?
Qual response recebe?
Busca uma impressão por vez ou várias?
Existe paginação?
Existe polling interval?
O intervalo vem do backend ou do agente?
```

---

## 6.10 Confirmação de impressão

Mapear como o agente informa que imprimiu.

Responder:

```text
Qual endpoint marca como impresso?
Qual status é gravado?
Existe data_hora_impressao?
Existe usuário/agente responsável?
Existe controle de sucesso?
Existe controle de erro?
Existe retry?
```

---

## 6.11 Tratamento de erro

Mapear como erros de impressão são tratados.

Responder:

```text
Se a impressora estiver desligada, o que acontece?
Se o agente não estiver rodando, o que acontece?
Se a impressão falhar, o status muda?
A fila tenta novamente?
Existe limite de tentativas?
Existe mensagem de erro?
Existe tela para visualizar falhas?
```

---

## 6.12 Payload de impressão

Mapear o formato do payload enviado para o agente.

Responder:

```text
O backend envia ESC/POS pronto?
O backend envia PDF?
O backend envia HTML?
O backend envia Base64?
O agente monta comandos de impressão ou só envia para impressora?
O payload tem nome da impressora?
O payload tem largura do papel?
O payload tem tipo de documento?
```

---

## 6.13 Impressora local versus impressora da fila

Responder:

```text
A impressora é definida no caixa?
A impressora é definida no agente?
A impressora é enviada no payload?
A impressora é buscada no banco?
Quem decide para qual impressora imprimir?
```

Essa resposta é essencial para a futura impressão de cozinha.

---

## 6.14 Suporte a múltiplas impressoras

Responder:

```text
O agente atual suporta múltiplas impressoras?
O agente consegue escolher impressora por nome?
O agente consegue imprimir em impressora de rede?
O agente consegue imprimir em impressora compartilhada no Windows?
Existe alguma limitação identificada?
```

---

## 6.15 Relação com caixa

Mapear a relação entre caixa e impressão.

Responder:

```text
A impressão sempre depende de caixa?
Cupom depende de caixa?
Pedido operacional depende de caixa?
Etiqueta depende de caixa?
Agente depende de caixa?
A chave de acesso está em tb_caixa?
O caixa define nome da impressora?
```

---

## 6.16 Relação com usuário logado

Responder:

```text
O backend usa o usuário logado para descobrir o caixa?
Como o caixa do usuário é obtido?
O pedido operacional usa o caixa do usuário?
O cupom usa o caixa do usuário?
O agente usa usuário logado ou apenas chave?
```

---

## 6.17 Endpoints existentes de impressão

Criar tabela com todos os endpoints encontrados.

Tabela obrigatória:

| Método | Endpoint | Autenticação | Usado por | Retorno | Observação |
|---|---|---|---|---|---|

---

## 6.18 Classes e métodos envolvidos

Criar tabela com as classes e métodos principais.

Tabela obrigatória:

| Classe | Método | Responsabilidade | Fluxo |
|---|---|---|---|

---

## 6.19 Tabelas envolvidas

Criar tabela com todas as tabelas relacionadas.

Tabela obrigatória:

| Tabela | Finalidade | Campos relevantes | Observação |
|---|---|---|---|

---

## 6.20 Enums e tipos de documento

Mapear enums relacionados a tipo de documento/status de impressão.

Responder:

```text
Existe EnumTipoDocumentoImpressao?
Quais IDs existem?
Existe tipo para cupom?
Existe tipo para pedido operacional?
Existe tipo para etiquetas?
Como adicionar novo tipo no futuro?
```

---

# 7. Análise de capacidade para cozinha/produção

Após mapear o fluxo atual, criar seção específica:

```text
Análise para futura impressão de cozinha/produção
```

Responder:

```text
O modelo atual suporta impressões por setor?
O modelo atual suporta múltiplos agentes?
O modelo atual suporta um agente por cozinha/bar/chapa?
O modelo atual suporta um agente central imprimindo em várias impressoras?
O modelo atual precisa ser evoluído?
Quais campos/tabelas faltam?
Quais riscos existem se tentarmos usar o idCaixa para cozinha?
```

---

# 8. Recomendações técnicas

Com base no mapeamento, sugerir uma arquitetura futura.

Não implementar, apenas recomendar.

Avaliar as opções:

## Opção A — Um agente por ponto físico

```text
Agente do caixa imprime caixa.
Agente da cozinha imprime cozinha.
Agente do bar imprime bar.
```

Avaliar:

```text
vantagens
desvantagens
impacto no backend
impacto no agente
impacto na fila
```

---

## Opção B — Um agente central com várias impressoras

```text
Um único agente consulta a fila e imprime em impressoras diferentes pelo nome.
```

Avaliar:

```text
vantagens
desvantagens
impacto no backend
impacto no agente
impacto na fila
limitações de rede/Windows
```

---

## Opção C — Modelo híbrido recomendado

```text
Permitir uma impressora para tudo em lojas pequenas.
Permitir terminais/pontos de impressão separados em lojas maiores.
```

Avaliar como o sistema atual precisaria evoluir para suportar isso.

---

# 9. Proposta conceitual futura

Criar uma proposta conceitual, sem implementação, usando os termos:

```text
Terminal de Impressão
Ponto de Impressão
Destino de Impressão
Setor de Preparo
```

Explicar se o melhor nome no contexto do projeto deveria ser:

```text
Terminal de Impressão
```

ou

```text
Ponto de Impressão
```

A proposta deve considerar:

```text
um terminal para tudo
um terminal por setor
vários setores apontando para o mesmo terminal
um agente consultando apenas impressões do terminal dele
```

---

# 10. Riscos identificados

Criar uma seção:

```text
Riscos se a cozinha for implementada sem evoluir o agente/fila
```

Listar riscos como:

```text
impressão sair no setor errado
impressão duplicada
pedido recusado imprimir antes da aceitação
cancelamento não chegar à cozinha
agente do caixa consumir impressão da cozinha
dificuldade de configurar múltiplas impressoras
dependência de computador local
falha de comunicação com impressora de rede
```

---

# 11. Resultado esperado do MD final

Criar um arquivo de documentação em:

```text
/skils/impressao/agente-impressao-mapeamento.md
```

Se a pasta não existir, criar.

O MD final deve conter:

1. Visão geral do fluxo atual.
2. Fluxo de cupom.
3. Fluxo de pedido operacional.
4. Fluxo do agente.
5. Fila de impressão.
6. Autenticação do agente.
7. Configuração local do agente.
8. Endpoints de consulta e confirmação.
9. Tabelas envolvidas.
10. Enums e tipos de documento.
11. Payload enviado ao agente.
12. Relação com caixa.
13. Relação com impressora.
14. Suporte ou não a múltiplas impressoras.
15. Riscos.
16. Lacunas.
17. Recomendações para evolução.
18. Proposta conceitual para cozinha/produção.
19. Lista de arquivos analisados.
20. Lista de pontos não encontrados.

---

# 12. Restrições

Não alterar código.

Não criar migration.

Não criar endpoint.

Não modificar agente.

Não modificar backend.

Não modificar frontend.

Não renomear métodos.

Não aplicar refatoração.

Apenas analisar e documentar.

---

# 13. Formato esperado da documentação

O MD deve ser objetivo, mas completo.

Usar tabelas para:

```text
endpoints
classes
métodos
tabelas
enums
status
riscos
recomendações
```

Usar fluxos em texto para processos ponta a ponta.

Exemplo:

```text
POST /cupom/{id}/imprimir
  → CupomController
  → CupomService
  → CupomReceiptViewRepository
  → gera ESC/POS
  → envia para fila
  → agente consulta pendência
  → agente imprime
  → agente confirma status
```

---

# 14. Critério de conclusão

A tarefa só estará concluída quando for possível responder com segurança:

```text
Como o agente atual decide quais impressões imprimir?
Como a fila atual é roteada?
Como o agente autentica?
Como o backend envia impressão para fila?
Como o agente confirma impressão?
O modelo atual suporta cozinha/bar/chapa?
O que precisa mudar para suportar impressão por setor?
```

---

# 15. Entrega final

Ao final, apresentar no terminal/resposta:

```text
Arquivo criado/atualizado:
/skils/impressao/agente-impressao-mapeamento.md
```

E um resumo com:

```text
principais descobertas
riscos encontrados
recomendação para próxima etapa
```

Não implementar nada além da documentação.
