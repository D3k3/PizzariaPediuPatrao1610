# PediuPatrão

Sistema de pedidos da pizzaria PediuPatrão, feito com Java 21, Spring Boot 3.4.4, Spring Security, Thymeleaf e MongoDB.

## Como executar

### Pré-requisitos

- Java 21
- Uma conta no MongoDB Atlas (ou um MongoDB local)
- Não precisa instalar o Maven: o projeto usa o Maven Wrapper (`mvnw`)

### Conexão com o MongoDB

1. No Atlas, crie um cluster e um usuário de banco com permissão de leitura e escrita.
2. Em Network Access, libere o seu IP.
3. Crie o arquivo `src/main/resources/application-local.properties` com a sua string de conexão:

```properties
spring.data.mongodb.uri=mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/?appName=PediuPatrao
```

### Rodar

```bash
./mvnw spring-boot:run
```

No Windows: `.\mvnw.cmd spring-boot:run`. Depois abra `http://localhost:8080`.

### Usuários de teste

Criados automaticamente na primeira execução:

| Usuário | Senha | Perfil |
|---|---|---|
| admin | PediuAdm26 | ADMIN |
| gerente | PediuGer26 | GERENTE |
| atendente | PediuAte26 | ATENDENTE |

## Requisitos implementados

### RF01 - Perfis e autorização

O sistema tem três perfis: ADMIN, GERENTE e ATENDENTE. As permissões são conferidas no backend, nas rotas do `SecurityConfig` e com `@PreAuthorize` nos services, e o menu só mostra o que cada perfil pode usar. Só o ADMIN gerencia usuários. Ninguém consegue alterar o próprio perfil ou username nem se excluir, e o último ADMIN não pode ser rebaixado nem removido.

### RF02 - Modelo do pedido

O pedido tem cliente, tipo de atendimento (entrega ou retirada) e uma lista de itens com produto e quantidade. Preço, subtotal e total são calculados no servidor a partir do cadastro do produto, e qualquer valor de total, desconto ou status mandado pelo cliente é ignorado. O sistema grava quem registrou o pedido e a data e hora de entrada.

### RF03 - Entrada, saída e transições

O pedido segue o fluxo RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA (entrega) ou RETIRADO (retirada) -> FINALIZADO. Transições fora dessa ordem são recusadas, e um pedido cancelado não muda mais. Na saída, o sistema grava a data e hora e o responsável, e toda mudança de status fica no histórico do pedido. GERENTE e ATENDENTE podem avançar o status.

### RF04 - Cancelamento e desconto

Só o GERENTE cancela e dá desconto, e só antes da saída do pedido. O cancelamento exige uma justificativa e mantém os itens e valores. O desconto é um percentual maior que 0 e até 20% sobre o pedido inteiro, e um novo desconto substitui o anterior.

### RF05 - Auditoria

Criação, alteração e exclusão de clientes e de usuários, e criação, mudança de status, desconto e cancelamento de pedidos geram um registro de auditoria. Cada registro guarda usuário, perfil, data e hora, operação, entidade, os campos alterados com valor anterior e novo, e a justificativa quando existe. Senhas nunca são gravadas, os registros não podem ser alterados e a consulta, com filtros e paginação, é liberada só para ADMIN e GERENTE, com o telefone mascarado.

## Principais alterações

Em relação ao projeto original:

**Configuração e segurança**
- `SecurityConfig`: regras de acesso por perfil e método HTTP, CSRF ativo e respostas 401/403 em JSON para a API.
- `DataInitializer`: cria os usuários admin, gerente e atendente.
- `MigracaoPerfisRunner` e `MigracaoPedidosRunner` (novos): ajustam perfis e status antigos que já estavam no banco.
- `RelogioConfig` (novo): relógio usado para registrar datas.
- `UsuarioLogado` (novo): lê o usuário e o perfil logados.
- `application.properties`: conexão pelo `application-local.properties` ou pela variável `MONGODB_URI`, sem usuário fixo.
- `pom.xml`: troca a dependência de JPA por validação, Thymeleaf com Spring Security e as dependências de teste.

**Entidades e DTOs**
- `Usuario` e `Perfil` (novo): o perfil passa a ser o enum `Perfil` e a senha não aparece no JSON.
- `Pedido`, `ItemPedido`, `HistoricoStatus`, `StatusPedido` e `TipoAtendimento` (novos, exceto `Pedido`): novo modelo do pedido com itens, valores, histórico e dados de saída e cancelamento.
- `Cliente`: método `copia()`, usado para comparar os dados antes e depois de uma alteração na auditoria.
- `Produto`: removida a anotação de JPA.
- `AuditoriaEvento`, `AlteracaoCampo`, `OperacaoAuditoria` e `EntidadeAuditada` (novos): registro de auditoria.
- `PedidoRequest`, `ItemPedidoRequest`, `CancelamentoRequest`, `DescontoRequest`, `UsuarioRequest`, `UsuarioResponse`, `FiltroAuditoria`, `PaginaResponse` e `AuditoriaEventoResponse` (novos): dados de entrada e saída da API.

**Services**
- `PedidoService` e `CalculoPedido` (novo): registro do pedido com cálculo dos valores.
- `StatusPedidoService` e `FluxoStatusPedido` (novos): avanço de status.
- `CancelamentoPedidoService` e `DescontoPedidoService` (novos): cancelamento e desconto.
- `AuditoriaService`, `ComparadorCampos` e `MascaraDados` (novos): gravação e consulta da auditoria.
- `UsuarioService`: gestão de usuários com as proteções do RF01 e auditoria.
- `UsuarioDetailsService`: login com o perfil gravado no banco.
- `ClienteService` e `ProdutoService`: validações, permissões e auditoria de clientes.

**Repositórios, controllers e erros**
- `UsuarioRepository`: busca por perfil e por username sem diferenciar maiúsculas.
- `AuditoriaRepository` e `AuditoriaConsultaRepository` (novos): gravação e consulta com filtros.
- `PedidoController`: rotas de registro, status, cancelamento e desconto.
- `UsuarioController`, `ClienteController` e `ProdutoController`: API ajustada às novas regras.
- `HomeController`: telas de usuários e clientes.
- `PedidoTelaController`, `ProdutoTelaController`, `AuditoriaController` e `AuditoriaTelaController` (novos): telas e API de pedidos, produtos e auditoria.
- `ApiExceptionHandler`, `ErroApi`, `RegraNegocioException` e `TransicaoStatusInvalidaException` (novos): respostas de erro com status e mensagem.

**Telas**
- `pedidos.html` e `pedidos/form.html` (novo): lista com ações de status, desconto e cancelamento, e formulário com itens.
- `auditoria/lista.html` (novo): consulta da auditoria.
- `usuarios/lista.html` e `usuarios/form.html`: gestão de usuários com perfil.
- `clientes/lista.html`, `clientes/form.html`, `produtos/lista.html` e `produtos/produto-form.html`: botões conforme o perfil e campos novos.
- `fragments/sidebar.html`: menu por perfil.
- `layout.html` e `static/js/api.js` (novo): envio do token CSRF nas chamadas da API.
- `login.html` e `error.html`: mensagens de erro e de acesso negado.

## Testes

```bash
./mvnw test
```

Os testes não precisam do MongoDB.

1. ATENDENTE cria pedido mandando total, status e desconto falsos no JSON e o sistema calcula 60,00, status RECEBIDO e sem desconto.
2. ADMIN não pode criar pedido (403).
3. Pedido avança de RECEBIDO para EM_PREPARACAO e ganha uma entrada no histórico.
4. Pedido não pode pular de PRONTO para FINALIZADO.
5. Saída para entrega grava a data de saída e o responsável.
6. Pedido cancelado não muda de status.
7. GERENTE cancela pedido com justificativa e o cancelamento vai para a auditoria.
8. ATENDENTE não pode cancelar pedido (403).
9. GERENTE não pode dar desconto de 25% (400).
10. Não dá para aplicar desconto em pedido que já saiu.
11. ATENDENTE altera o telefone de um cliente e a auditoria guarda o valor anterior e o novo.
12. ATENDENTE não pode consultar a auditoria (403).
13. GERENTE não pode abrir a tela de usuários (403).
14. GERENTE não pode criar usuário ADMIN pela API (403).
15. ADMIN não pode trocar o próprio perfil.
