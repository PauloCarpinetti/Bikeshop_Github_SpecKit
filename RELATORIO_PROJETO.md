# Relatório do Projeto BikeShop

## Visão Geral

Este relatório documenta o que foi realizado no projeto BikeShop, desde a formalização inicial da especificação até o planejamento técnico, quebra em tarefas e o início da implementação real (Fase 1 e Fase 2 do plano).

---

## Sessão 2026-07-09 — Descoberta e definição

### 1. Definição da constituição do projeto

Foi criada uma constituição inicial do projeto com princípios estruturantes para desenvolvimento, qualidade e governança, incluindo:

- Test-First (não negociável)
- Privacidade e proteção de dados por design
- Controle de acesso baseado em papéis
- Desenvolvimento modular por funcionalidade
- Observabilidade e confiabilidade
- Simplicidade e acessibilidade
- Padrões não funcionais
- Workflow de desenvolvimento em Scrum
- Governança e regras de evolução da constituição

Arquivo: [.specify/memory/constitution.md](.specify/memory/constitution.md)

### 2. Geração da especificação inicial do produto

Especificação funcional cobrindo personas, histórias de usuário, requisitos funcionais/não funcionais, entidades de domínio, critérios de sucesso e premissas de escopo.

Arquivo: [specs/001-bike-shop-ecommerce/spec.md](specs/001-bike-shop-ecommerce/spec.md)

---

## Sessão 2026-07-10 — Planejamento técnico, tasks, análise e implementação inicial

### 3. Plano de implementação (`/speckit.plan`)

Com base no `spec.md` e no `TechStack.pdf` (stack de referência fornecida), foi gerado o plano técnico completo:

- [specs/001-bike-shop-ecommerce/plan.md](specs/001-bike-shop-ecommerce/plan.md) — Technical Context, Constitution Check (todos os 6 princípios avaliados), estrutura de pastas (`backend/`, `frontend/`, `infra/`)
- [specs/001-bike-shop-ecommerce/research.md](specs/001-bike-shop-ecommerce/research.md) — decisões e racional por tecnologia (Next.js, Spring Boot, MySQL/Redis, Meilisearch, RabbitMQ, gateways de pagamento, notificações)
- [specs/001-bike-shop-ecommerce/data-model.md](specs/001-bike-shop-ecommerce/data-model.md) — entidades de domínio (Produto, Variação, Cliente, Carrinho, Pedido, Cupom, Avaliação, Log de Auditoria, Endereço)
- [specs/001-bike-shop-ecommerce/quickstart.md](specs/001-bike-shop-ecommerce/quickstart.md) — roteiro de validação dos 3 cenários prioritários
- [specs/001-bike-shop-ecommerce/contracts/api-overview.md](specs/001-bike-shop-ecommerce/contracts/api-overview.md) — superfície de endpoints REST por módulo

**Decisão de negócio confirmada durante a sessão**: transportadora de frete = **Correios** (não constava no TechStack.pdf; foi uma pendência levantada e depois confirmada pelo usuário).

### 4. Quebra em tarefas (`/speckit.tasks`)

Geração de [specs/001-bike-shop-ecommerce/tasks.md](specs/001-bike-shop-ecommerce/tasks.md): 95 tarefas organizadas em 6 fases (Setup, Foundational, User Story 1/2/3, Polish), com testes incluídos por história (Test-First não negociável).

### 5. Análise de consistência (`/speckit.analyze`)

Primeira rodada encontrou 7 achados, incluindo **1 CRITICAL**: a tarefa de orquestração do checkout (US1/MVP) dependia implicitamente de um serviço de cupom que só seria criado na fase de backoffice (US3), quebrando a entrega independente do MVP. Outros achados: moderação de avaliações ausente no backoffice, e-mail de confirmação de pedido só implementado na US2, ausência de tela de recuperação de checkout, ambiguidade sobre "gerenciar clientes básicos", metas de performance vagas, e uma entidade (Endereço) fora das Key Entities do spec.

**Todas as correções foram aplicadas** (`tasks.md`, `spec.md`, `plan.md`, `contracts/api-overview.md`), com 6 novas tarefas adicionadas (T044b, T046b, T070b, T070c, T079b, T081b). Uma segunda rodada de análise confirmou **0 issues críticos, 100% de cobertura de requisitos**.

### 6. Implementação (`/speckit.implement`) — Fase 1 (Setup) e Fase 2 (Foundational)

Optamos por implementar em fases, parando em checkpoints testáveis em vez de tentar as 101 tarefas de uma vez. Hoje foram concluídas as tarefas **T001–T018**:

**Backend** (`backend/`, Spring Boot 3 / Java 21):
- `pom.xml` com Spring Web, Data JPA, Security, Redis, AMQP, Flyway, MapStruct, Springdoc OpenAPI, Meilisearch client, Sentry, Actuator/Prometheus
- Segurança: JWT stateless (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`), papéis RBAC (`CUSTOMER`, `OPERATOR`, `ADMIN`)
- Tratamento global de erros (`GlobalExceptionHandler`, `ApiError`, `BusinessException`)
- Configuração de RabbitMQ (exchange + 3 filas: orders/inventory/notifications), Redis, Meilisearch, OpenAPI/Swagger
- Módulo de auditoria completo (`AuditLog`, `AuditLogRepository`, `AuditService`) + migração Flyway baseline (`V1__init.sql`)
- Teste de contexto (smoke test) validando toda a Fase 2

**Frontend** (`frontend/`, Next.js 14 / TypeScript):
- Projeto Next.js com Tailwind, React Hook Form + Zod, Sentry
- `apiClient.ts` base com validação de erro via Zod
- Lint/format (ESLint, Prettier)

**Infraestrutura**:
- `infra/docker-compose.yml` (MySQL, Redis, RabbitMQ, Meilisearch)
- Pipeline de CI (`.github/workflows/ci.yml`)

Todas as 18 tarefas foram marcadas `[X]` em `tasks.md`.

### 7. Validação de ponta a ponta

Subimos a infraestrutura real e validamos:
- `mvn test` → smoke test passou (contexto Spring sobe com Security, JPA/Flyway, Redis, RabbitMQ, Meilisearch)
- Containers Docker (MySQL, Redis, RabbitMQ, Meilisearch) → todos `healthy`
- Backend rodando (`http://localhost:8081`, Swagger em `/swagger-ui/index.html`)
- Frontend rodando (`http://localhost:3002`)

**Três ajustes necessários por conflitos no ambiente local** (não relacionados ao código em si):
1. MySQL: porta 3306 já usada por um MySQL nativo → container remapeado para **3307**
2. Backend: porta 8080 já usada pelo container `evolution_api` (outro projeto) → backend movido para **8081**
3. Frontend: porta 3000 já usada pelo container `evolution_manager` (outro projeto) → frontend fixado na **3002**
4. Adicionado `allowPublicKeyRetrieval=true` na URL JDBC (exigência do MySQL 8.4 com autenticação `caching_sha2_password`)

### 8. Discussão sobre a Fase 3 (User Story 1 / MVP)

Fase 3 tem 31 tarefas (catálogo+busca, carrinho, frete, checkout, 3 gateways de pagamento, pedidos, autenticação, notificações, 6 páginas de frontend) — bem mais complexa que a Fase 2. Decidido dividir em três sub-blocos menores: **3A** (catálogo + carrinho, sem dependências externas), **3B** (frete + checkout + pagamento, o mais pesado) e **3C** (autenticação + fechamento do MVP), cada um com checkpoint próprio em `tasks.md`.

---

## Sessão 2026-07-13 — Sub-bloco 3A (Catálogo e Carrinho)

Implementadas as 12 tarefas do sub-bloco 3A:

**Backend**: `Produto`/`VariacaoProduto` (entidades + Flyway V2), `ProductSearchService` com Meilisearch real (busca full-text + facetas), `ProductController`, `Carrinho`/`ItemCarrinho` no Redis (cookie `bikeshop_cart_id`), `CartService` com validação de estoque, `CartController`, `CatalogDataSeeder` (4 produtos de exemplo), CORS configurado entre frontend (3002) e backend.

**Frontend**: página de catálogo com busca/filtros, página de detalhe com variações/specs/geometria, carrinho (Context + Drawer), header com contador.

**Bugs reais encontrados e corrigidos**: `GlobalExceptionHandler` engolia exceções sem logar; `LazyInitializationException` no `CartService` (corrigido com `@Transactional`); `GenericJackson2JsonRedisSerializer` sem tipagem (carrinho virava `LinkedHashMap` genérico ao voltar do Redis); `TestRestTemplate` sem suporte a PATCH (faltava `httpclient5`).

Validado com testes automatizados (5) e navegador (fluxo completo: busca → detalhe → adicionar → atualizar quantidade → remover).

---

## Sessão 2026-08-21 — Sub-bloco 3B (Frete, Checkout e Pagamento)

Implementadas as 10 tarefas do sub-bloco 3B, sem credenciais reais de gateway (usuário optou por seguir sem elas):

**Frete**: `ShippingProvider` + `CorreiosShippingProvider` — calcula peso cubado (fórmula padrão dos Correios) e cai para uma estimativa local quando não há credenciais reais dos Correios configuradas (mesmo padrão de resiliência do Meilisearch). Novas colunas `peso_kg`/`altura_cm`/`largura_cm`/`comprimento_cm` em `variacao_produto` (Flyway V3).

**Pedidos**: `Pedido`/`ItemPedido` com ciclo de vida de status e histórico (JSON), preço congelado no momento da compra.

**Pagamentos**: `PaymentGatewayAdapter` (Strategy) com 3 implementações reais (Stripe, Mercado Pago, PagSeguro) que chamam a API oficial de cada gateway quando há chave configurada, e caem em **modo simulado** (status `PENDING`, referência `SIM-<provider>-<uuid>`) quando não há — incluindo verificação de assinatura de webhook (HMAC) para Stripe e Mercado Pago.

**Checkout**: `CheckoutService` orquestra tudo numa transação só — calcula frete, debita estoque, cria o pedido, inicia o pagamento e limpa o carrinho.

**Frontend**: página de checkout (React Hook Form + Zod) com resumo do carrinho, cálculo de frete e seleção de gateway; tela de recuperação (`/checkout/recovery`) para quando o pedido falha.

**Bugs reais encontrados e corrigidos**:
1. **Dados de peso/dimensão incorretos** — o MySQL já tinha produtos da sessão 3A (volume Docker persistente); a migração V3 só adicionou colunas com `DEFAULT` nas linhas existentes, e o seeder não roda de novo se a tabela não está vazia. Resultado: todo item calculava frete com peso genérico (1kg/15x30x90cm) em vez do peso real. Corrigido truncando as tabelas e deixando o seeder rodar de novo.
2. **Badge do carrinho não atualizava após o checkout** — o `CartContext` não tinha como ser "avisado" que o carrinho foi limpo no backend. Adicionado `refresh()` exposto pelo contexto, chamado pela página de checkout após o pedido ser criado.

**Conflitos de porta** (mais dois projetos locais seus, `chat_mysql`/`chat_redis`/`chat_backend`, entraram em conflito): MySQL `3307→3308`, backend `8081→8082`, Redis `6379→6380`.

Validado com testes automatizados (5 novos, 11 no total) e navegador (checkout completo: endereço → frete real por peso → pagamento simulado → pedido criado → carrinho limpo).

---

## Sessão 2026-08-23 — Sub-bloco 3C (Autenticação e fechamento do MVP)

Implementadas as 8 tarefas do sub-bloco 3C, sem credenciais externas (JWT é gerado localmente; SendGrid segue o mesmo padrão de resiliência dos demais integrações — usuário confirmou que ainda não tem nenhuma credencial):

**Autenticação**: entidade `Cliente` (Flyway V4, e-mail único, senha com BCrypt), `AuthController` com `POST /auth/register`, `/auth/login` e `/auth/refresh` emitindo JWT de acesso e de refresh (`JwtService` já existente da Fase 2).

**Merge de carrinho**: `CartService.mergeIntoCustomerCart` — ao cadastrar ou logar, o carrinho de visitante (cookie `bikeshop_cart_id`) é somado ao carrinho do cliente (chave `customer:<id>` no Redis, somando quantidades de itens repetidos) e o carrinho da sessão atual passa a refletir o resultado mesclado.

**Eventos e notificação**: `CheckoutService` publica `InventoryAdjustedEvent` (por item debitado) e `OrderCreatedEvent` (ao concluir o pedido) via `DomainEventPublisher`/RabbitMQ; `OrderConfirmationListener` consome `OrderCreatedEvent` tipado e dispara e-mail transacional via SendGrid quando há API key configurada, ou loga um aviso simulado quando não há.

**Observabilidade**: logging estruturado no `CheckoutService` (início do checkout, frete calculado, checkout concluído com valores).

**Frontend**: `AuthContext` (JWT + dados do cliente persistidos em `localStorage`), páginas `/login` e `/register`, `Header` mostrando "Olá, {nome}" / "Sair" quando autenticado, checkout pré-preenchendo nome/e-mail do cliente logado, teste E2E Playwright (`checkout-journey.spec.ts`) cobrindo catálogo → carrinho → cadastro com merge → checkout → confirmação.

**Bugs reais encontrados e corrigidos**:
1. **Acessibilidade (WCAG / Princípio VI da constituição)** — `<label>` sem `htmlFor`/`id` associado aos `<input>` nas páginas de login, cadastro e checkout (encontrado em autorrevisão, antes de rodar testes). Corrigido em todos os campos das 3 páginas.
2. **Redesenho do `OrderConfirmationListener`** — a primeira versão recebia o payload do RabbitMQ como `String` bruto e tentava extrair a routing key do corpo da mensagem (routing key não faz parte do corpo — erro de design, corrigido antes de rodar). Redesenhado para consumir o record tipado `OrderCreatedEvent` diretamente, usando a desserialização tipada do Spring AMQP (`__TypeId__`).
3. **Checkout não pré-preenchia nome/e-mail do cliente logado** — encontrado ao validar no navegador: `AuthContext` carrega o cliente do `localStorage` de forma assíncrona (`useEffect`), mas o `useForm` do checkout só lê `defaultValues` na primeira renderização (quando o cliente ainda é `null`) e o React Hook Form não reage a mudanças posteriores em `defaultValues`. Corrigido com um `useEffect` que chama `setValue` quando o cliente fica disponível.

Validado com testes automatizados (2 novos — `AuthContractTest` — 13 no total, incluindo confirmação via log de que o evento `OrderCreatedEvent` percorre o RabbitMQ ponta a ponta) e navegador: cadastro com carrinho de visitante mesclado corretamente, checkout com nome/e-mail pré-preenchidos, frete recalculado, pedido criado como cliente autenticado, logout e login validados.

**Follow-up conhecido (não bloqueia)**: o teste E2E Playwright (T023, `checkout-journey.spec.ts`) cobre o mesmo fluxo e está escrito, mas não roda dentro do sandbox de execução deste agente — `npx playwright test` falha com `spawn UNKNOWN` ao iniciar o `chrome.exe`, tanto via Git Bash quanto via PowerShell nativo, com ou sem sandbox desabilitado. O binário do Chromium foi baixado com sucesso; a falha é ao repassar os pipes de depuração remota do Chromium para o processo filho, uma restrição do próprio sandbox do agente ao criar subprocessos, não do shell escolhido nem do código da aplicação. Rodar `npm run test:e2e` diretamente num terminal do usuário (fora do agente) deve funcionar normalmente.

---

## Sessão 2026-08-23 (continuação) — Sub-bloco 4A (Perfil, Endereços e Histórico de Pedidos)

Implementadas as 9 tarefas do sub-bloco 4A (Fase 4 dividida em 4A/4B, mesmo racional da Fase 3):

**Backend**: `Endereco` (Flyway V5, FK para `cliente`) + `telefone` em `Cliente`; `CustomerProfileService` (perfil, troca de senha opcional, CRUD de endereços com controle de "endereço padrão"); `AccountController` (`GET/PUT /account/profile`, `GET/POST/PUT /account/addresses`); `OrderQueryService` + `AccountOrdersController` (`GET /account/orders`, `GET /account/orders/{orderId}`, com verificação de propriedade — pedido de outro cliente retorna "não encontrado", não vaza existência). `OrderDto` passou a incluir `criadoEm`, `statusHistorico` e `enderecoEntrega` (necessários para a tela de rastreamento).

**Frontend**: página `/profile` (dados cadastrais + lista/criação/edição de endereços) e páginas `/orders` (histórico) e `/orders/[id]` (detalhe com linha do tempo de status e endereço de entrega); `Header` ganhou links "Olá, {nome}" → `/profile` e "Meus pedidos" → `/orders`.

**Bugs reais encontrados e corrigidos**:
1. **Gap descoberto na implementação**: o checkout nunca vinculava `Pedido.clienteId` ao cliente autenticado (sempre gravava `null`), mesmo logado — o histórico de pedidos ficaria sempre vazio. Corrigido propagando o `Authentication` do `CheckoutController` até `OrderService.criarPedido` (guest checkout continua funcionando; `clienteId` fica `null` só para visitante).
2. **Mismatch de schema na migração** — `V5__account.sql` criou `endereco.estado` como `CHAR(2)`, mas a entidade JPA mapeia como `VARCHAR` por padrão; Hibernate recusou subir com `SchemaManagementException`. Corrigido trocando para `VARCHAR(2)` (H2, usado nos testes, não pegou o mismatch — só apareceu ao subir contra o MySQL real).

Validado com testes automatizados (5 novos — `AccountContractTest`, `AccountOrdersContractTest` — 18 no total) e navegador: perfil consultado/atualizado, endereço criado, pedido novo criado como cliente autenticado aparecendo no histórico com rastreamento e endereço corretos, acesso a pedido de outro cliente corretamente negado.

---

## Sessão 2026-08-23 (continuação) — Sub-bloco 4B (Avaliações, Trocas/Devoluções e Notificação de Status)

Implementadas as 10 tarefas do sub-bloco 4B, fechando a Fase 4 (User Story 2):

**Backend**: `Avaliacao` (Flyway V6) + `ReviewService` — só permite avaliar um item de um pedido do próprio cliente confirmado como `ENTREGUE`, valida que o item pertence ao pedido, e rejeita avaliação duplicada (mesmo cliente/produto/pedido). `ReturnService` — solicitação de troca/devolução só para pedidos entregues, muda o status para `EM_TROCA_DEVOLUCAO` e grava o motivo no Log de Auditoria (FR-011) via `AuditService` já existente da Fase 2. `PostSaleController` expõe `POST /account/orders/{orderId}/return` e `POST /account/reviews`. `OrderStatusChangedEvent`/`OrderStatusChangedListener` notificam o cliente por e-mail (SendGrid, simulado sem credenciais) a cada mudança de status do pedido — reaproveitando um `SendGridEmailSender` extraído do listener de confirmação de pedido da Fase 3C.

**Frontend**: `ReturnRequestForm` e `ReviewForm` (novos componentes) integrados na página de detalhe do pedido — aparecem quando o pedido está `ENTREGUE`; ao solicitar devolução, a UI reflete a mudança de status imediatamente sem recarregar a página.

**Bugs reais encontrados e corrigidos**:
1. **Máquina de estados do pedido bloqueava a ramificação de pós-venda** — `ENTREGUE` já era tratado como estado totalmente terminal (nenhuma saída permitida), mas o data-model exige a ramificação `entregue → em_troca_devolução`. Corrigido com uma exceção específica a essa transição em `OrderService.validarTransicao`.
2. **Risco de consumidores concorrentes na mesma fila** — a fila `orders.events` estava vinculada ao padrão `orders.*`, que capturaria tanto `orders.created` quanto o novo `orders.status-changed`; dois listeners competindo pela mesma fila causariam falha de desserialização quando o tipo de evento não batesse com o esperado. Corrigido criando uma fila e um binding dedicados (`orders.status.events` / `orders.status-changed`) e tornando o binding original específico (`orders.created`), evitando a sobreposição — encontrado por análise antes de rodar, não em produção.

Validado com testes automatizados (2 novos — `PostSaleContractTest` — 20 no total, incluindo confirmação via log de que ambos os listeners de notificação recebem exatamente o evento esperado em filas separadas) e navegador: avaliação publicada, solicitação de troca/devolução registrada com motivo auditado, rastreamento do pedido atualizado em tempo real na UI.

**Follow-up conhecido (não bloqueia)**: como não há backoffice ainda (Fase 5), não existe forma de levar um pedido a `ENTREGUE` pela UI — o E2E Playwright (T051) cobre o que é alcançável hoje (cadastro, compra, atualização de conta/endereço, histórico), e a validação em navegador acima usou um `UPDATE` SQL direto para simular a entrega, só para exercitar a UI de pós-venda manualmente.

---

## Sessão 2026-08-23 (continuação) — Sub-bloco 5A (Produtos e Estoque)

Implementadas as 6 tarefas do sub-bloco 5A, abrindo a Fase 5 (backoffice, dividida em 5A/5B/5C, mesmo racional das Fases 3 e 4):

**Backend**: `ProductAdminService` — CRUD de produto/variação (slug gerado automaticamente a partir do nome, com desambiguação), soft delete (inativa em vez de apagar, preservando o histórico de pedidos que referenciam o produto) e reindexação automática no Meilisearch a cada mutação (remove do índice quando inativado ou sem variação ativa — FR-001). `ProductAdminController` (`GET/POST/PUT/DELETE /admin/products`, `POST/PUT .../variants`) e `StockAdminController` (`PATCH /admin/products/{sku}/stock`, publica o mesmo evento `inventory.adjusted` usado pelo checkout). `AdminUserSeeder` cria um usuário `ADMIN` de desenvolvimento no primeiro start (pré-requisito: não havia forma de criar um usuário administrativo até aqui).

**Frontend**: `/admin/products` (listar, criar, editar produto e variações) e `/admin/inventory` (ajuste de estoque por SKU com motivo); `useRequireAdmin` protege as duas páginas no cliente (o backend já recusa via RBAC independentemente). Header ganhou o link "Backoffice" para quem é `ADMIN`/`OPERATOR`.

**Bugs reais encontrados e corrigidos**:
1. **Colisão de rota** — `(admin)/products` e `(admin)/inventory` foram inicialmente criados como route groups (parênteses não entram na URL), mas isso colidiria com `(shop)/products` já existente, ambos resolvendo para `/products`. Corrigido usando um segmento real (`app/admin/products/`, `app/admin/inventory/`) antes mesmo de compilar.
2. **Mutadores `package-private` inacessíveis** — os métodos de atualização adicionados a `Produto`/`VariacaoProduto` seguiram o padrão já usado em `Pedido`/`Cliente` (visibilidade de pacote), mas `ProductAdminService` vive num pacote (`admin`) diferente de `catalog` — corrigido tornando-os `public`, já que agora há um chamador legítimo fora do pacote.

Validado com testes automatizados (2 novos — `ProductAdminContractTest` — 22 no total) e navegador: produto criado (com slug automático) aparecendo no catálogo público, edição refletida, variação adicionada e editada, estoque ajustado (+7, refletido no catálogo), produto inativado sumindo da busca pública, e acesso negado tanto para visitante (frontend redireciona ao login) quanto para cliente comum (backend 403).

---

## Sessão 2026-08-23 (continuação) — Sub-bloco 5B (Pedidos e Cupons)

Implementadas as 9 tarefas do sub-bloco 5B:

**Backend**: `OrderAdminService`/`OrderAdminController` (`GET/PATCH /admin/orders`) — reaproveita `OrderService.atualizarStatus` (já valida a transição e notifica o cliente, T066) e grava a ação no Log de Auditoria (FR-011), diferente da transição automática do checkout/webhook. `CupomDesconto` (Flyway V7, com `usos_realizados`) + `CouponService` — valida validade, valor mínimo do carrinho, categoria aplicável e limite de uso, rejeitando com mensagem clara (edge case da spec); `CouponAdminController` para CRUD e `POST /checkout/coupon` (em `CheckoutController`) para validação/preview. `Pedido` ganhou `valorDesconto` (nova coluna) e o método `aplicarCupom`, aplicado em `POST /checkout/orders` antes da criação da intenção de pagamento (para o valor cobrado já refletir o desconto) — `OrderDto` passou a expor `valorDesconto`, `cupomCodigo`, `clienteNome` e `clienteEmail` (necessários para a UI administrativa identificar o dono do pedido).

**Frontend**: `/admin/orders` (listar pedidos com dados do cliente, atualizar status) e `/admin/coupons` (criar/listar/desativar cupons); campo de cupom integrado à página de checkout do cliente, com preview do desconto antes de finalizar o pedido.

**Bugs reais encontrados e corrigidos**:
1. **Bug de fuso horário no formulário de cupom** — `toDatetimeLocal` fatiava a string ISO (UTC) direto para o input `datetime-local` (que sempre representa hora local, sem timezone), deslocando a data de início do cupom para o futuro dependendo do fuso do navegador; o cupom nascia com "válido de" ainda não alcançado e era rejeitado como expirado. Corrigido compensando o offset de timezone antes de formatar.
2. **Falso alarme durante a validação em navegador**: o clique automatizado no botão "Aplicar" do cupom não disparava o handler de forma confiável (mesma flakiness de automação já registrada em sessões anteriores) — confirmado como problema da ferramenta de automação, não da aplicação, ao disparar o clique via `element.click()` diretamente.

Validado com testes automatizados (4 novos — `OrderAdminContractTest`, `CouponAdminContractTest` — 26 no total, incluindo o edge case de cupom expirado) e navegador: cupom criado e aplicado no checkout com o valor de desconto correto (15% calculado e persistido), contador de uso incrementado, pedido listado no backoffice com dados do cliente e cupom aplicado visíveis, status atualizado com entrada correspondente no Log de Auditoria.

---

## Estado atual do projeto

| Item | Status |
|---|---|
| Constituição | ✅ Definida |
| Especificação (spec.md) | ✅ Completa, revisada e corrigida |
| Plano técnico (plan.md + research/data-model/quickstart/contracts) | ✅ Completo |
| Backlog de tarefas (tasks.md) | ✅ 101 tarefas, consistência validada — **81 concluídas** |
| Fase 1 — Setup | ✅ Implementada e validada |
| Fase 2 — Foundational | ✅ Implementada e validada |
| Fase 3A — Catálogo e Carrinho | ✅ Implementada e validada |
| Fase 3B — Frete, Checkout e Pagamento | ✅ Implementada e validada (pagamento/frete reais pendentes de credenciais) |
| Fase 3C — Autenticação e fechamento do MVP | ✅ Implementada e validada (**MVP / User Story 1 completo**) |
| Fase 4A — Perfil, Endereços e Histórico de Pedidos | ✅ Implementada e validada |
| Fase 4B — Avaliações, Trocas/Devoluções e Notificação de Status | ✅ Implementada e validada (**User Story 2 completa**) |
| Fase 5A — Produtos e Estoque | ✅ Implementada e validada |
| Fase 5B — Pedidos e Cupons | ✅ Implementada e validada |
| Fase 5C — Clientes, Auditoria, Moderação e Fechamento | ⏳ Não iniciada |
| Fase 6 — Polish | ⏳ Não iniciada |

## URLs de desenvolvimento local (atuais)

> Atualizadas na sessão do sub-bloco 3B: novos conflitos de porta apareceram com outros projetos locais (`chat_backend`, `chat_mysql`, `chat_redis`), então backend/MySQL/Redis mudaram de porta novamente.

- Backend: `http://localhost:8082` (Swagger: `/swagger-ui/index.html`)
- Frontend: `http://localhost:3002`
- MySQL (container): porta `3308`
- Redis: `6380` · RabbitMQ: `5672` (management `15672`) · Meilisearch: `7700`

## Próximos passos recomendados

### 1. Preparar credenciais externas (fazer em paralelo, não depende de código)

- Criar contas sandbox no **Stripe**, **Mercado Pago** e **PagSeguro** e obter chaves de teste — os adapters já estão prontos, só falta configurar as variáveis de ambiente (`STRIPE_SECRET_KEY`, `MERCADOPAGO_ACCESS_TOKEN`, `PAGSEGURO_TOKEN`, etc. em `backend/.env.example`)
- Obter usuário/senha da API oficial dos Correios (`CORREIOS_API_USUARIO`/`CORREIOS_API_SENHA`) — hoje o frete usa uma estimativa local por peso cubado
- Obter uma API key do **SendGrid** (`SENDGRID_API_KEY`) para envio real do e-mail de confirmação de pedido — hoje o envio é simulado (apenas logado)

### 2. Sub-bloco 5C — Fecha o Backoffice (User Story 3)

Clientes (consulta/bloqueio), log de auditoria, moderação de avaliações, guardas de RBAC formais no frontend (`features/admin/guards.ts`, consolidando o `useRequireAdmin` já usado desde a 5A) e o E2E completo do backoffice — já detalhados em `tasks.md`. Com `/admin/orders` já permitindo levar um pedido a `ENTREGUE` (5B), o teste E2E completo de pós-venda da Fase 4B (T051) já pode ser refeito sem o `UPDATE` SQL manual usado até aqui.

### 3. Fase 6 — Polish

Dashboards de observabilidade, logs centralizados, auditoria de acessibilidade, hardening de segurança e validação final de performance contra os critérios de sucesso do `spec.md`.

## Resumo executivo

O MVP (User Story 1) e a conta do cliente/pós-venda (User Story 2) estão completos, e o backoffice (User Story 3) está quase completo: catálogo com busca, carrinho, checkout com pagamento, frete e cupons de desconto (pagamento/frete simulados, prontos para credenciais reais), autenticação com merge de carrinho, perfil/endereços, histórico de pedidos com rastreamento, avaliações de produto, solicitação de troca/devolução com auditoria e notificação de status, e gestão administrativa de produtos/estoque/pedidos/cupons — tudo testado via testes automatizados e navegador. Resta clientes/auditoria/moderação no backoffice, e os itens de polimento — já detalhados em `tasks.md`.
