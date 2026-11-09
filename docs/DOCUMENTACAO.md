# PediuPatrão — Dashboard Administrativo

Documentação do Dashboard Administrativo da PediuPatrão (API REST): requisitos, endpoints e evidências de testes.

Sumário: [1. Requisitos](#1-requisitos-funcionais-e-não-funcionais) · [2. Endpoints](#2-documentação-dos-endpoints) · [3. Evidências de testes](#3-evidências-de-testes)

---

## 1. Requisitos funcionais e não funcionais

### 1.1 Glossário e regras de negócio

| Termo | Definição |
|-------|-----------|
| **Venda** | Pedido com `status = FINALIZADO`. |
| **Valor da venda** | `valorTotal` do pedido (já com desconto). |
| **Faturamento** | Soma do valor das vendas do período. |
| **Nº de vendas** | Quantidade de vendas do período. |
| **Ticket médio** | `faturamento / nº de vendas`; **0** se não houver vendas. |
| **Total de descontos** | Soma de `valorBruto − valorTotal` das vendas do período. |
| **Cancelamentos** | Quantidade de pedidos `CANCELADO` com `dataEntrada` no período. |
| **Período** | Filtra por `dataEntrada`, intervalo inclusivo `[início 00:00:00, fim 23:59:59.999]` em `America/Sao_Paulo`. Padrão: últimos 30 dias (incluindo hoje). Máximo: 366 dias. O fim não pode estar no futuro. Só `fim` informado: início = fim − 29 dias. Só `inicio` informado: fim = hoje. |
| **Tipo de atendimento** | Filtro opcional `ENTREGA` ou `RETIRADA`, aplicado a todos os endpoints (inclusive últimos pedidos). Pedido sem `tipoAtendimento` fica de fora quando há filtro. |
| **Receita do produto** | Soma, nas vendas do período, dos `subtotal` dos itens do produto, com o desconto do pedido rateado pelo subtotal: `receita_item = subtotal × (valorTotal / valorBruto)`. O último item do pedido absorve a diferença de centavos para que a soma dos itens seja igual a `valorTotal`. |
| **Picos e rankings** | Consideram apenas vendas (`FINALIZADO`), agrupadas por `dataEntrada` no fuso do `Clock`. Empate no pico: maior faturamento, depois a hora/dia mais cedo. Sem vendas: pico `null`. |
| **Cancelados** | Aparecem somente em `quantidadeCancelados` e no gráfico de pedidos por status (que conta todos os status). |
| **Arredondamento** | 2 casas, `HALF_UP` (mesmo de `CalculoPedido`). Dinheiro em `BigDecimal`. |
| **Pedidos sem itens** | Pedidos sem `itens` (legado, campos antigos `sabor`/`tipoPizza`) ou sem `valorTotal` são **desconsiderados em todos os cálculos** do dashboard (KPIs, séries, status, rankings, picos e últimos pedidos). |

### 1.2 Requisitos funcionais

| ID | Requisito | Critério de aceite |
|----|-----------|--------------------|
| RF01 | Exibir KPIs: faturamento, nº de vendas, ticket médio, cancelamentos e total de descontos. | Valores batem com cálculo manual sobre um conjunto conhecido de pedidos; ticket médio = 0 sem vendas. |
| RF02 | Exibir faturamento por dia. | Uma entrada por dia do período, inclusive dias sem venda (valor 0); soma da série = faturamento (RF01). |
| RF03 | Exibir quantidade de pedidos por status. | Inclui todos os 7 status (zero quando vazio) e inclui CANCELADO; soma = total de pedidos considerados do período. |
| RF04 | Exibir ranking de produtos **ordenável por receita ou por quantidade**, nos sentidos maior e menor (a tela usa: top 10 maiores, com seletor RECEITA/QUANTIDADE, e os 5 menores por receita). | `ordenarPor` = RECEITA ou QUANTIDADE e `ordem` = MAIOR ou MENOR; `limite` entre 1 e 20 (padrão 10); receita conforme regra de rateio; desempate por nome; produtos sem venda no período não entram; soma das receitas = faturamento. |
| RF05 | Filtrar por **período** (data inicial e final) **e por tipo de atendimento** (ENTREGA/RETIRADA). | Padrão 30 dias; início > fim, período > 366 dias, fim no futuro, data mal formatada ou tipo de atendimento desconhecido → erro 400 com mensagem clara; todos os blocos respeitam os filtros; tipo ausente = todos. |
| RF06 | Integrar a tela à API do dashboard. | A tela só consome a API; nenhum cálculo de negócio no navegador. |
| RF07 | Botão "Atualizar", indicador de carregamento, mensagens de erro e estado vazio. | Loading visível; erro (401/403/500/rede) exibe mensagem e permite tentar de novo; período sem vendas exibe estado vazio. |
| RF08 | Listar os 5 últimos pedidos finalizados com o valor de cada um comparado ao ticket médio. | Ordenados pela data em que passaram a `FINALIZADO` (histórico), mais recente primeiro; ticket médio de comparação = últimos 30 dias; cada item traz `valorTotal`, diferença vs. ticket médio (R$ e %) e id curto; **sem dados do cliente**; ignora o período, respeita o tipo de atendimento. |
| RF09 | Exibir horários de pico. | Vendas agrupadas por hora cheia (0–23) de `dataEntrada`; 24 posições com nº de vendas e faturamento; informa a hora de pico. |
| RF10 | Exibir dias de pico. | Vendas agrupadas por dia da semana de `dataEntrada`; 7 posições (segunda a domingo) com nº de vendas e faturamento; informa o dia de pico. |

### 1.3 Requisitos não funcionais

| ID | Requisito | Critério de aceite |
|----|-----------|--------------------|
| RNF01 | Acesso restrito a ADMIN e GERENTE, validado no backend. | ATENDENTE recebe 403 e anônimo recebe 401 (JSON `ErroApi`, sem redirecionamento) na API; regra em `SecurityConfig` e `@PreAuthorize` no controller. |
| RNF02 | Todos os cálculos no servidor. | Respostas da API já trazem os valores finais. |
| RNF03 | Fuso `America/Sao_Paulo` em período, agrupamentos por dia/hora e `Clock`. | Pedido às 23:30 locais conta no dia/hora correto; `RelogioConfig` usa `ZoneId` explícito. |
| RNF04 | Tempo de resposta inferior a 2 s por endpoint com o volume de demonstração (~120 pedidos). | Cada endpoint responde em menos de 2 s com os dados do seed de demonstração. |
| RNF05 | Nenhum dado pessoal de cliente na API do dashboard. | Resposta não contém `clienteId`, nome, telefone ou endereço. |
| RNF06 | Dinheiro em `BigDecimal` com 2 casas; JSON serializa como número. | Sem erros de ponto flutuante. |

### 1.4 Decisões de projeto e limitações

- **Últimos pedidos (RF08):** a data de finalização fica dentro de `historicoStatus` e não pode ser ordenada diretamente na consulta. O sistema busca os 100 pedidos `FINALIZADO` com `dataEntrada` mais recente (`findTop100ByStatusOrderByDataEntradaDesc`) e ordena em memória pela data da transição para `FINALIZADO`. Um pedido muito antigo que seja finalizado agora, e que esteja fora dos 100 mais recentes por entrada, pode não aparecer. Pedido finalizado sem a entrada `FINALIZADO` no histórico é ignorado.
- O filtro `tipoAtendimento` é aplicado em memória, depois da consulta por período.

---

## 2. Documentação dos endpoints

Base: `/api/dashboard`. Todos são `GET`, retornam `application/json` e exigem sessão autenticada com perfil **ADMIN** ou **GERENTE**. Valores monetários e percentuais são números crus com 2 casas.

### 2.1 Autenticação nos exemplos (`curl`)

A API usa a sessão do login por formulário. Faça login no navegador e copie o valor do cookie `JSESSIONID` (DevTools → Application → Cookies), ou obtenha-o por `curl` (o login exige o token CSRF da página `/login`). Nos exemplos, `SESSAO` é esse valor:

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/resumo"
```

### 2.2 Parâmetros comuns (endpoints 1 a 6)

| Parâmetro | Tipo | Padrão | Regras |
|-----------|------|--------|--------|
| `inicio` | `yyyy-MM-dd` | hoje − 29 | opcional |
| `fim` | `yyyy-MM-dd` | hoje | opcional; não pode estar no futuro |
| `tipoAtendimento` | `ENTREGA` \| `RETIRADA` | ausente = todos | opcional |

### 2.3 Erros comuns

Todos os erros usam o corpo `ErroApi`: `{"status": <código>, "erro": "<mensagem>", "caminho": "<uri>"}`.

**400 — requisição inválida** (exemplos reais, cobertos pelos testes):

```json
{"status":400,"erro":"A data inicial não pode ser posterior à data final.","caminho":"/api/dashboard/resumo"}
{"status":400,"erro":"O período não pode exceder 366 dias.","caminho":"/api/dashboard/resumo"}
{"status":400,"erro":"A data final não pode estar no futuro.","caminho":"/api/dashboard/resumo"}
{"status":400,"erro":"Valor inválido para o parâmetro 'tipoAtendimento'.","caminho":"/api/dashboard/resumo"}
{"status":400,"erro":"Valor inválido para o parâmetro 'inicio'.","caminho":"/api/dashboard/resumo"}
{"status":400,"erro":"Valor inválido para o parâmetro 'ordenarPor'.","caminho":"/api/dashboard/produtos"}
{"status":400,"erro":"limite deve estar entre 1 e 20.","caminho":"/api/dashboard/produtos"}
```

**401 — sem login** (nunca redireciona para `/login`):

```json
{"status":401,"erro":"Não autenticado","caminho":"/api/dashboard/resumo"}
```

**403 — perfil sem permissão (ATENDENTE):**

```json
{"status":403,"erro":"Acesso negado","caminho":"/api/dashboard/resumo"}
```

### 2.4 `GET /api/dashboard/resumo`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento`.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/resumo?inicio=2026-10-10&fim=2026-11-08&tipoAtendimento=ENTREGA"
```

```json
{
  "periodo": { "inicio": "2026-10-10", "fim": "2026-11-08" },
  "faturamento": 18450.90,
  "quantidadeVendas": 87,
  "ticketMedio": 212.08,
  "quantidadeCancelados": 9,
  "totalDescontos": 640.10
}
```

Erros: 400 (período/tipo), 401, 403.

### 2.5 `GET /api/dashboard/vendas-por-dia`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento`. Uma entrada por dia, zeros nos dias sem venda.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/vendas-por-dia?inicio=2026-11-01&fim=2026-11-03"
```

```json
[
  { "data": "2026-11-01", "faturamento": 0.00, "quantidade": 0 },
  { "data": "2026-11-02", "faturamento": 520.00, "quantidade": 3 },
  { "data": "2026-11-03", "faturamento": 310.50, "quantidade": 2 }
]
```

Erros: 400, 401, 403.

### 2.6 `GET /api/dashboard/pedidos-por-status`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento`. Sempre os 7 status, na ordem do enum, contando todos os pedidos considerados do período.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/pedidos-por-status"
```

```json
[
  { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 2 },
  { "status": "EM_PREPARACAO", "descricao": "Em preparação", "quantidade": 0 },
  { "status": "PRONTO", "descricao": "Pronto", "quantidade": 1 },
  { "status": "SAIU_PARA_ENTREGA", "descricao": "Saiu para entrega", "quantidade": 0 },
  { "status": "RETIRADO", "descricao": "Retirado", "quantidade": 1 },
  { "status": "FINALIZADO", "descricao": "Finalizado", "quantidade": 87 },
  { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 9 }
]
```

Erros: 400, 401, 403.

### 2.7 `GET /api/dashboard/produtos`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento` e:

| Parâmetro | Valores | Padrão | Limites |
|-----------|---------|--------|---------|
| `ordenarPor` | `RECEITA` \| `QUANTIDADE` | `RECEITA` | enum inválido → 400 |
| `ordem` | `MAIOR` \| `MENOR` | `MAIOR` | enum inválido → 400 |
| `limite` | inteiro | `10` | 1 a 20 |

Desempate por nome. Só produtos com venda no período. Usos da tela: top 10 `MAIOR` (seletor RECEITA/QUANTIDADE) e top 5 `MENOR` por `RECEITA`.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/produtos?ordenarPor=RECEITA&ordem=MENOR&limite=5"
```

```json
[
  { "produtoId": "6650f1a2b3c4d5e6f7a8b9c1", "nome": "Pizza Calabresa", "quantidadeVendida": 41,
    "receita": 2350.75, "percentualDoFaturamento": 12.74 }
]
```

Erros: 400 (inclusive `limite` e enums), 401, 403.

### 2.8 `GET /api/dashboard/picos/horarios`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento`. 24 posições (hora 0–23); `horaPico` é `null` sem vendas.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/picos/horarios"
```

```json
{
  "horaPico": 20,
  "itens": [
    { "hora": 0, "quantidade": 0, "faturamento": 0.00 },
    { "hora": 20, "quantidade": 14, "faturamento": 3120.00 },
    { "hora": 23, "quantidade": 1, "faturamento": 45.00 }
  ]
}
```

(O exemplo abrevia os itens; a resposta real traz as 24 posições.) Erros: 400, 401, 403.

### 2.9 `GET /api/dashboard/picos/dias-semana`

Perfis: ADMIN, GERENTE · Parâmetros: `inicio`, `fim`, `tipoAtendimento`. 7 posições, de `MONDAY` a `SUNDAY`; `diaPico` é `null` sem vendas.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/picos/dias-semana"
```

```json
{
  "diaPico": "SATURDAY",
  "itens": [
    { "dia": "MONDAY", "nome": "Segunda-feira", "quantidade": 6, "faturamento": 1180.40 },
    { "dia": "SATURDAY", "nome": "Sábado", "quantidade": 21, "faturamento": 4390.00 },
    { "dia": "SUNDAY", "nome": "Domingo", "quantidade": 18, "faturamento": 3650.20 }
  ]
}
```

(Resposta real: 7 itens.) Erros: 400, 401, 403.

### 2.10 `GET /api/dashboard/ultimos-pedidos`

Perfis: ADMIN, GERENTE · Parâmetro: `tipoAtendimento` (opcional). Ignora `inicio`/`fim`. Retorna até 5 pedidos `FINALIZADO` mais recentes pela data da transição para `FINALIZADO`; a comparação usa o ticket médio dos últimos 30 dias. **Nunca** retorna nome, telefone, endereço ou id do cliente.

```bash
curl -s -b "JSESSIONID=SESSAO" "http://localhost:8080/api/dashboard/ultimos-pedidos?tipoAtendimento=ENTREGA"
```

```json
[
  { "id": "6650f1a2b3c4d5e6f7a8b9c0", "idCurto": "6650f1a2",
    "dataFinalizacao": "2026-11-08T21:14:03", "tipoAtendimento": "ENTREGA",
    "quantidadeItens": 3, "valorTotal": 245.00,
    "diferencaTicketMedio": 32.92, "diferencaTicketMedioPercentual": 15.52 }
]
```

Erros: 400 (`tipoAtendimento`), 401, 403. Limitação: ver 1.4.

---

## 3. Evidências de testes

Data da execução: **2026-11-09** · Comando: `./mvnw clean test`

### 3.1 Lista dos testes

**Demais classes:** `DashboardSeedTest` (7), `CancelamentoEDescontoTest` (2), `StatusPedidoServiceTest` (4), `UsuarioEClienteServiceTest` (2), `RotasEPermissoesTest` (7).

**`DashboardServiceTest` (30)** — Mockito com `Clock` fixo (2026-11-08, America/Sao_Paulo):
- Resumo: só FINALIZADO conta; ticket médio 0 sem vendas; total de descontos; pedido sem itens/sem valorTotal ignorado.
- Filtro `tipoAtendimento` em todos os cálculos; limites do período (início do dia a fim do dia).
- Período: início > fim; > 366 dias; exatamente 366 válido; **só `inicio` com mais de 366 dias até hoje → exceção**; só `fim` usa 30 dias para trás; fim no futuro.
- Vendas por dia com zeros; pedidos por status com os 7 valores.
- Produtos: rateio do desconto e centavos no último item; rateio proporcional; ranking RECEITA/QUANTIDADE × MAIOR/MENOR; limite e padrões; desempate por nome; limite fora de 1..20; cancelados não entram.
- Picos: 24 posições e hora correta; 23:30 conta na hora 23; sem vendas → pico nulo; 7 posições de segunda a domingo e dia correto; empate decidido por faturamento.
- Últimos pedidos: 5 ordenados pela data de finalização com diferença correta; filtro por tipo e pedido sem finalização no histórico ignorado; sem vendas nos 30 dias → diferença percentual 0.

**`DashboardControllerTest` (15)** — `@WebMvcTest`, service mockado, `@WithMockUser`:
- ADMIN 200 + formato do JSON de `/resumo`; GERENTE 200.
- ATENDENTE 403 com `ErroApi` (resumo e todos os endpoints).
- Anônimo 401 com `ErroApi`, sem `Location` (resumo e todos os endpoints).
- `inicio > fim` 400; `limite=50` 400; `limite=0` 400; `ordenarPor` inválido 400; `ordem` inválida 400; `tipoAtendimento` inválido 400; data mal formatada 400.
- Formato do JSON de `/produtos`; repasse dos parâmetros ao service.

### 3.2 Resultado final

```text
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.config.DashboardSeedTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.controller.DashboardControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.service.CancelamentoEDescontoTest
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.service.DashboardServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.service.StatusPedidoServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.service.UsuarioEClienteServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.web.RotasEPermissoesTest
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Total: **67 testes**, 0 falhas.
