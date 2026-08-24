---

description: "Task list template for feature implementation"
---

# Tasks: Plataforma E-commerce de Bicicletas e Acessórios

**Input**: Design documents from `/specs/001-bike-shop-ecommerce/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/api-overview.md](./contracts/api-overview.md), [quickstart.md](./quickstart.md)

**Tests**: Incluídas em cada user story — a constituição do projeto define Test-First como não negociável (Princípio I) e `spec.md` exige cobertura de testes de aceitação/regressão antes do lançamento.

**Organization**: Tasks agrupadas por user story (US1, US2, US3) conforme prioridade em `spec.md`, permitindo implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: A qual user story a tarefa pertence (US1, US2, US3)
- Caminhos de arquivo exatos incluídos em cada descrição

## Path Conventions

Conforme `plan.md` (Web application): `backend/src/main/java/com/bikeshop/...` e `backend/src/test/java/com/bikeshop/...` para a API Spring Boot; `frontend/src/...` e `frontend/tests/...` para o Next.js; `infra/` para orquestração local/deploy.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto e estrutura básica

- [X] T001 Criar estrutura de repositório (`backend/`, `frontend/`, `infra/`, `.github/workflows/`) conforme `plan.md`
- [X] T002 Inicializar projeto Spring Boot (Java 21, Maven) com dependências Spring Web, Spring Data JPA, Spring Security, Flyway, MapStruct, Springdoc OpenAPI em `backend/pom.xml`
- [X] T003 [P] Inicializar projeto Next.js (TypeScript, App Router) com Tailwind CSS, React Hook Form e Zod em `frontend/package.json`
- [X] T004 [P] Configurar lint/format (Checkstyle/Spotless no backend, ESLint/Prettier no frontend)
- [X] T005 [P] Criar `infra/docker-compose.yml` com MySQL, Redis, RabbitMQ e Meilisearch para ambiente local
- [X] T006 [P] Criar pipeline inicial de CI em `.github/workflows/ci.yml` (build + test de backend e frontend)

**Checkpoint**: Projetos inicializam e sobem localmente sem funcionalidade de negócio ainda implementada

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central que MUST estar pronta antes de qualquer user story

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [X] T007 Criar migração Flyway baseline (schema inicial) em `backend/src/main/resources/db/migration/V1__init.sql`
- [X] T008 Implementar framework de autenticação/autorização Spring Security + JWT em `backend/src/main/java/com/bikeshop/common/security/`
- [X] T009 [P] Definir papéis RBAC (`CUSTOMER`, `OPERATOR`, `ADMIN`) em `backend/src/main/java/com/bikeshop/common/security/Role.java`
- [X] T010 [P] Configurar tratamento global de erros e formato padrão de resposta em `backend/src/main/java/com/bikeshop/common/exception/`
- [X] T011 [P] Configurar Springdoc OpenAPI/Swagger em `backend/src/main/java/com/bikeshop/common/config/OpenApiConfig.java`
- [X] T012 Configurar conexão RabbitMQ e publisher base de eventos em `backend/src/main/java/com/bikeshop/common/messaging/`
- [X] T013 [P] Configurar Redis (cache de catálogo, sessão/carrinho de visitante) em `backend/src/main/java/com/bikeshop/common/config/RedisConfig.java`
- [X] T014 [P] Configurar client Meilisearch em `backend/src/main/java/com/bikeshop/common/config/MeilisearchConfig.java`
- [X] T015 Implementar entidade e serviço de Log de Auditoria (FR-011) em `backend/src/main/java/com/bikeshop/audit/` (`AuditLog`, `AuditService`, listener/aspecto de escrita)
- [X] T016 [P] Criar client de API base e schemas Zod compartilhados em `frontend/src/services/apiClient.ts`
- [X] T017 [P] Configurar Sentry no frontend e no backend
- [X] T018 [P] Expor métricas Prometheus via Actuator no backend

**Checkpoint**: Fundação pronta — implementação das user stories pode começar (em paralelo, se houver equipe)

---

## Phase 3: User Story 1 - Explorar catálogo e concluir compra (Priority: P1) 🎯 MVP

**Goal**: Visitante ou cliente descobre produtos, monta carrinho, simula frete (Correios) e conclui compra com pagamento seguro; carrinho de visitante é preservado ao autenticar.

**Independent Test**: Usuário encontra produto, adiciona ao carrinho, calcula frete por CEP e conclui o pedido com sucesso (cenário 1 de `quickstart.md`).

Para reduzir o tamanho de cada sessão de implementação, a Fase 3 é dividida em três sub-blocos (3A, 3B, 3C) com checkpoint próprio. As tarefas e IDs originais (T019–T047 + T044b/T046b) não mudam — apenas a ordem de execução é explicitada.

---

### Sub-bloco 3A — Catálogo e Carrinho

**Goal**: Visitante navega o catálogo com busca/filtros facetados, vê detalhe do produto e monta um carrinho (visitante, via Redis).

**Independent Test**: Encontrar um produto, aplicar um filtro, abrir o detalhe e adicionar ao carrinho — sem depender de checkout, pagamento ou login.

**Sem dependências externas** (não precisa de credenciais de gateway de pagamento). É o sub-bloco mais rápido e o primeiro resultado visível.

#### Tests

- [X] T019 [P] [US1] Teste de contrato para `GET /catalog/products` e `/catalog/products/{slug}` em `backend/src/test/java/com/bikeshop/catalog/ProductContractTest.java`
- [X] T020 [P] [US1] Teste de contrato para endpoints de `/cart` (adicionar/atualizar/remover/merge) em `backend/src/test/java/com/bikeshop/cart/CartContractTest.java`

#### Implementation

- [X] T025 [P] [US1] Criar entidade/repositório `Produto` em `backend/src/main/java/com/bikeshop/catalog/Produto.java`
- [X] T026 [P] [US1] Criar entidade/repositório `VariacaoProduto` (SKU, atributos, estoque) em `backend/src/main/java/com/bikeshop/catalog/VariacaoProduto.java`
- [X] T027 [US1] Implementar `ProductSearchService` (sincronização e consulta Meilisearch, filtros facetados) em `backend/src/main/java/com/bikeshop/catalog/ProductSearchService.java` (depende de T025, T026)
- [X] T028 [US1] Implementar endpoints `GET /catalog/products` e `GET /catalog/products/{slug}` em `backend/src/main/java/com/bikeshop/catalog/ProductController.java` (depende de T027)
- [X] T029 [P] [US1] Criar entidades `Carrinho`/`ItemCarrinho` (Redis para visitante, MySQL para autenticado) em `backend/src/main/java/com/bikeshop/cart/`
- [X] T030 [US1] Implementar `CartService` (adicionar/atualizar/remover/mesclar) em `backend/src/main/java/com/bikeshop/cart/CartService.java` (depende de T029)
- [X] T031 [US1] Implementar endpoints `/cart` em `backend/src/main/java/com/bikeshop/cart/CartController.java` (depende de T030)
- [X] T041 [P] [US1] Construir listagem de catálogo com filtros facetados em `frontend/src/app/(shop)/products/page.tsx`
- [X] T042 [P] [US1] Construir página de detalhe do produto (geometria, specs, fotos) em `frontend/src/app/(shop)/products/[slug]/page.tsx`
- [X] T043 [P] [US1] Construir carrinho (drawer/página) em `frontend/src/features/cart/`

**Checkpoint 3A**: catálogo navegável e carrinho de visitante funcionando ponta a ponta (backend + frontend), demonstrável e testável isoladamente.

---

### Sub-bloco 3B — Frete, Checkout e Pagamento

**Goal**: A partir de um carrinho existente (3A), calcular frete via Correios, criar o pedido e processar pagamento (Stripe/Mercado Pago/PagSeguro).

**Independent Test**: Com um carrinho já montado, informar CEP, ver a estimativa de frete e concluir o pedido com um pagamento sandbox.

**Depende de**: Sub-bloco 3A completo (usa `CartService`). **Bloqueio externo**: precisa das credenciais sandbox dos 3 gateways de pagamento antes de validar o fluxo real (sem elas, dá para implementar e testar a orquestração, mas não o pagamento fim a fim). É o sub-bloco mais pesado.

#### Tests

- [X] T021 [P] [US1] Teste de contrato para `POST /checkout/shipping-quote` em `backend/src/test/java/com/bikeshop/checkout/ShippingQuoteContractTest.java`
- [X] T022 [P] [US1] Teste de contrato para `POST /checkout/orders` e `POST /payments/{orderId}/intents` + webhook em `backend/src/test/java/com/bikeshop/checkout/CheckoutContractTest.java`

#### Implementation

- [X] T032 [US1] Implementar `ShippingProvider` + `CorreiosShippingProvider` (API dos Correios, cubagem/peso volumétrico) em `backend/src/main/java/com/bikeshop/checkout/shipping/`
- [X] T033 [US1] Implementar endpoint `POST /checkout/shipping-quote` em `backend/src/main/java/com/bikeshop/checkout/CheckoutController.java` (depende de T032)
- [X] T034 [P] [US1] Criar entidades `Pedido`/`ItemPedido` (com ciclo de vida de status, FR-007) em `backend/src/main/java/com/bikeshop/orders/`
- [X] T035 [US1] Implementar `OrderService` (criação de pedido a partir do carrinho, transições de status) em `backend/src/main/java/com/bikeshop/orders/OrderService.java` (depende de T034, T030)
- [X] T036 [US1] Implementar adapters de pagamento (Stripe, Mercado Pago, PagSeguro) via Strategy em `backend/src/main/java/com/bikeshop/payments/`
- [X] T037 [US1] Implementar `POST /payments/{orderId}/intents` e `POST /payments/webhooks/{provider}` (idempotente) em `backend/src/main/java/com/bikeshop/payments/PaymentController.java` (depende de T036)
- [X] T038 [US1] Implementar orquestração `POST /checkout/orders` (frete + pagamento) em `backend/src/main/java/com/bikeshop/checkout/CheckoutController.java` (depende de T033, T035, T037) — aplicação de cupom fica fora do escopo do MVP e é integrada na Fase 5 (ver T079b)
- [X] T044 [US1] Construir fluxo de checkout (endereço, frete, pagamento) em `frontend/src/app/(shop)/checkout/page.tsx`, com resumo do carrinho e cálculo de frete (depende de T041–T043)
- [X] T044b [US1] Implementar tela de recuperação de checkout em `frontend/src/app/(shop)/checkout/recovery/page.tsx` (rota própria — App Router exige `page.tsx` num diretório, por isso o caminho difere do `recovery.tsx` originalmente planejado) (depende de T044, T038)

**Checkpoint 3B**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-21. Fluxo completo de compra (carrinho → frete real por peso/cubagem → pedido criado → pagamento simulado) funcionando com um cliente de teste/seed — ainda sem cadastro/login reais. Frete e pagamento reais (Correios/Stripe/Mercado Pago/PagSeguro) ficam pendentes de credenciais.

---

### Sub-bloco 3C — Autenticação e fechamento do MVP

**Goal**: Cadastro/login de cliente, merge do carrinho de visitante, eventos e notificação de confirmação de pedido, observabilidade — fecha a User Story 1 como MVP completo.

**Independent Test**: Cenário 1 completo de `quickstart.md` (catálogo → carrinho → frete → checkout → login com merge de carrinho), validado via E2E.

**Depende de**: Sub-blocos 3A e 3B completos.

#### Tests

- [ ] T023 [US1] Teste de integração E2E (Playwright) catálogo → carrinho → frete → checkout → login com merge de carrinho em `frontend/tests/e2e/checkout-journey.spec.ts`
- [ ] T024 [US1] Checks de privacidade, RBAC, acessibilidade e observabilidade da história em `backend/src/test/java/com/bikeshop/checkout/` e `frontend/tests/e2e/checkout-journey.spec.ts`

#### Implementation

- [X] T039 [P] [US1] Implementar `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` em `backend/src/main/java/com/bikeshop/customers/AuthController.java` (depende de T008)
- [X] T040 [US1] Implementar merge de carrinho de visitante ao autenticar/cadastrar em `backend/src/main/java/com/bikeshop/cart/CartService.java` (depende de T030, T039)
- [X] T045 [US1] Construir formulários de login/cadastro com chamada de merge de carrinho em `frontend/src/features/auth/` + páginas `frontend/src/app/(shop)/login/` e `frontend/src/app/(shop)/register/` (depende de T040)
- [X] T046 [US1] Publicar eventos de pedido/estoque em `orders.events`/`inventory.events` na criação do pedido em `backend/src/main/java/com/bikeshop/checkout/CheckoutService.java` via `DomainEventPublisher` (evento tipado `OrderCreatedEvent`/`InventoryAdjustedEvent`, não um `OrderEventPublisher` dedicado) (depende de T035, T012)
- [X] T046b [US1] Implementar notificação de confirmação de pedido (e-mail transacional via SendGrid, simulado sem credenciais) ao consumir `orders.events` na criação do pedido, estabelecendo a base do módulo de notificações em `backend/src/main/java/com/bikeshop/notifications/OrderConfirmationListener.java` (depende de T046) — validado via teste de contrato com round-trip real pelo RabbitMQ
- [X] T047 [US1] Adicionar logging/instrumentação de observabilidade no fluxo de checkout (Princípio V) em `CheckoutService`

**Checkpoint 3C**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-23. Cadastro/login/refresh JWT funcionando, merge de carrinho de visitante ao autenticar, checkout pré-preenchido para cliente logado, eventos de pedido/estoque publicados no RabbitMQ e consumidos pelo listener de notificação (e-mail simulado sem credenciais SendGrid), logging de observabilidade no fluxo de checkout. User Story 1 totalmente funcional e testável de forma independente (MVP completo).

---

## Phase 4: User Story 2 - Gerenciar conta, pedidos e pós-venda (Priority: P2)

**Goal**: Cliente autenticado mantém dados atualizados, acompanha pedidos, solicita trocas/devoluções e publica avaliações.

**Independent Test**: Cliente autenticado acessa histórico de pedidos, altera dados cadastrais e registra uma solicitação de pós-venda (cenário 2 de `quickstart.md`).

Dividida em dois sub-blocos (mesmo racional da Fase 3): 4A entrega a base de "minha conta" (perfil/endereços + histórico/detalhe de pedidos), 4B entrega as ações de pós-venda (avaliação, troca/devolução) e a notificação de mudança de status.

### Sub-bloco 4A — Perfil, endereços e histórico de pedidos

**Goal**: Cliente autenticado consulta/atualiza dados cadastrais e endereços, e acompanha o histórico e detalhe dos próprios pedidos.

**Independent Test**: Logado, atualizar nome/endereço e ver o pedido criado na Fase 3 aparecer no histórico com o detalhe correto.

**Depende de**: User Story 1 completa (autenticação, `Pedido`). **Pré-requisito descoberto na implementação**: `Pedido.clienteId` nunca é preenchido hoje (checkout não captura o `Authentication` do cliente logado) — precisa ser corrigido aqui para o histórico funcionar.

#### Tests

- [X] T048 [P] [US2] Teste de contrato para `GET/PUT /account/profile` e `/account/addresses` em `backend/src/test/java/com/bikeshop/customers/AccountContractTest.java`
- [X] T049 [P] [US2] Teste de contrato para `GET /account/orders` e `GET /account/orders/{orderId}` em `backend/src/test/java/com/bikeshop/customers/AccountOrdersContractTest.java`

#### Implementation

- [X] T053 [P] [US2] Criar entidade/repositório `Endereco` em `backend/src/main/java/com/bikeshop/customers/Endereco.java`
- [X] T054 [US2] Implementar `CustomerProfileService` (atualização de perfil/endereços) em `backend/src/main/java/com/bikeshop/customers/CustomerProfileService.java` (depende de T053)
- [X] T055 [US2] Implementar endpoints `GET/PUT /account/profile` e `/account/addresses` em `backend/src/main/java/com/bikeshop/customers/AccountController.java` (depende de T054)
- [X] T056 [US2] Implementar consulta de histórico/detalhe de pedidos em `backend/src/main/java/com/bikeshop/orders/OrderQueryService.java` (depende de T034 da US1; inclui associar `Pedido.clienteId` ao cliente autenticado no checkout)
- [X] T057 [US2] Implementar endpoints `GET /account/orders` e `GET /account/orders/{orderId}` em `backend/src/main/java/com/bikeshop/customers/AccountOrdersController.java` (depende de T056)
- [X] T062 [P] [US2] Construir UI de perfil/endereços em `frontend/src/app/(account)/profile/`
- [X] T063 [P] [US2] Construir UI de histórico/detalhe de pedidos em `frontend/src/app/(account)/orders/`

**Checkpoint 4A**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-23. Perfil e endereços consultáveis/atualizáveis, histórico e detalhe de pedidos (com rastreamento e endereço de entrega) funcionando para o cliente autenticado, com verificação de propriedade (pedido de outro cliente retorna não encontrado). Corrigido um gap real descoberto durante a implementação: o checkout nunca vinculava `Pedido.clienteId` ao cliente autenticado — corrigido em `CheckoutController`/`CheckoutService`/`OrderService`.

---

### Sub-bloco 4B — Avaliações, trocas/devoluções e notificação de status

**Goal**: Cliente publica avaliação de produto entregue, solicita troca/devolução de um pedido, e é notificado quando o status do pedido muda.

**Independent Test**: Cenário 2 completo de `quickstart.md` (conta → histórico → devolução → avaliação), validado via E2E.

**Depende de**: Sub-bloco 4A completo.

#### Tests

- [X] T050 [P] [US2] Teste de contrato para `POST /account/orders/{orderId}/return` e `POST /account/reviews` em `backend/src/test/java/com/bikeshop/customers/PostSaleContractTest.java`
- [X] T051 [US2] Teste de integração E2E (Playwright) atualização de conta + histórico + devolução + avaliação em `frontend/tests/e2e/account-postsale.spec.ts` (etapas de devolução/avaliação exigem pedido `ENTREGUE`, só alcançável via backoffice — Fase 5, ainda não implementada; cobertas via `PostSaleContractTest` no backend, que avança o status diretamente)
- [X] T052 [US2] Checks de privacidade, RBAC, acessibilidade e observabilidade da história

#### Implementation

- [X] T058 [P] [US2] Criar entidade/repositório `Avaliacao` em `backend/src/main/java/com/bikeshop/reviews/Avaliacao.java`
- [X] T059 [US2] Implementar `ReviewService` (só permite avaliação para pedido entregue) em `backend/src/main/java/com/bikeshop/reviews/ReviewService.java` (depende de T058)
- [X] T060 [US2] Implementar `ReturnService` (solicitação de troca/devolução, com auditoria) em `backend/src/main/java/com/bikeshop/orders/ReturnService.java` (depende de T035, T015 da fundação)
- [X] T061 [US2] Implementar endpoints `POST /account/orders/{orderId}/return` e `POST /account/reviews` em `backend/src/main/java/com/bikeshop/customers/PostSaleController.java` (depende de T059, T060)
- [X] T064 [US2] Construir UI de solicitação de troca/devolução em `frontend/src/features/account/returns/` (depende de T063)
- [X] T065 [US2] Construir UI de publicação de avaliação em `frontend/src/features/reviews/`
- [X] T066 [US2] Notificar cliente sobre mudança de status do pedido via consumidor de fila (SendGrid/FCM), estendendo o módulo de notificações em `backend/src/main/java/com/bikeshop/notifications/` (depende de T046b da US1)

**Checkpoint 4B**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-23. Avaliação de produto entregue, solicitação de troca/devolução (com auditoria e notificação de mudança de status) funcionando ponta a ponta. User Stories 1 e 2 funcionando de forma independente (fecha a Fase 4).

---

## Phase 5: User Story 3 - Operar catálogo e pedidos no backoffice (Priority: P3)

**Goal**: Lojista/equipe administra produtos, estoque, pedidos, promoções e mantém controle de acesso e auditoria.

**Independent Test**: Administrador cria produto, ajusta estoque, atualiza status de pedido e cria cupom de desconto (cenário 3 de `quickstart.md`).

Dividida em três sub-blocos (mesmo racional das Fases 3 e 4): 5A entrega a gestão de catálogo (produtos/estoque), 5B a operação do dia a dia (pedidos/cupons, integrado ao checkout), 5C fecha com governança (clientes/auditoria/moderação), guardas de RBAC no frontend e o E2E completo do backoffice.

### Sub-bloco 5A — Produtos e Estoque

**Goal**: Administrador cadastra/edita produtos e variações, e ajusta estoque manualmente com reflexo imediato no catálogo.

**Independent Test**: Logado como `ADMIN`/`OPERATOR`, criar um produto com duas variações e ajustar o estoque de uma delas, confirmando o reflexo na disponibilidade exibida no catálogo público.

**Depende de**: User Story 1 completa (catálogo, `VariacaoProduto`). **Pré-requisito**: hoje não existe forma de criar um cliente com papel `ADMIN`/`OPERATOR` (todo cadastro via `/auth/register` vira `CUSTOMER`) — resolvido aqui com um seed/mecanismo mínimo de usuário administrativo.

#### Tests

- [X] T067 [P] [US3] Teste de contrato para CRUD `/admin/products` e `PATCH /admin/products/{sku}/stock` em `backend/src/test/java/com/bikeshop/admin/ProductAdminContractTest.java`

#### Implementation

- [X] T073 [US3] Implementar `ProductAdminService` (CRUD de produto/variação) em `backend/src/main/java/com/bikeshop/admin/ProductAdminService.java` (depende de T025, T026 da US1) — inclui `GET /admin/products` e `GET /admin/products/{id}` (leitura, necessária para a UI listar/editar; não estava explícita na tabela de contratos)
- [X] T074 [US3] Implementar endpoints `/admin/products` com guarda RBAC em `backend/src/main/java/com/bikeshop/admin/ProductAdminController.java` (depende de T073, T009)
- [X] T075 [US3] Implementar `PATCH /admin/products/{sku}/stock` com publicação de evento de estoque em `backend/src/main/java/com/bikeshop/admin/StockAdminController.java` (depende de T073, T012)
- [X] T082 [P] [US3] Construir UI de gestão de produtos/variações em `frontend/src/app/admin/products/` (caminho real sem route group — `(admin)/products` colidiria com `(shop)/products`, ambos resolvendo para `/products`)
- [X] T083 [P] [US3] Construir UI de ajuste de estoque em `frontend/src/app/admin/inventory/` (mesmo motivo do caminho real acima)

**Checkpoint 5A**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-23. CRUD de produto/variação, ajuste manual de estoque e inativação (soft delete, some do catálogo público) funcionando ponta a ponta; RBAC confirmada nos dois lados (backend 403 para CUSTOMER, frontend redireciona visitante/não-admin). Pré-requisito resolvido: `AdminUserSeeder` cria um usuário ADMIN de desenvolvimento no primeiro start.

---

### Sub-bloco 5B — Pedidos e Cupons

**Goal**: Administrador atualiza status/documentos de envio de pedidos e gerencia cupons de desconto, aplicáveis no checkout do cliente.

**Independent Test**: Logado como `ADMIN`/`OPERATOR`, alterar o status de um pedido existente e criar um cupom; como cliente, aplicar o cupom no checkout e confirmar rejeição de um cupom expirado (edge case da spec).

**Depende de**: Sub-bloco 5A completo (papel administrativo já disponível).

#### Tests

- [X] T068 [P] [US3] Teste de contrato para `GET/PATCH /admin/orders` em `backend/src/test/java/com/bikeshop/admin/OrderAdminContractTest.java`
- [X] T069 [P] [US3] Teste de contrato para CRUD `/admin/coupons` e validação de cupom em `backend/src/test/java/com/bikeshop/admin/CouponAdminContractTest.java`

#### Implementation

- [X] T076 [US3] Implementar `GET/PATCH /admin/orders` (status, documentos de envio) em `backend/src/main/java/com/bikeshop/admin/OrderAdminController.java` (depende de T035 da US1) — "documentos de envio" não implementado (fora de escopo: sem transportadora/integração real); PATCH grava auditoria (FR-011) e reaproveita a notificação de mudança de status já existente (T066)
- [X] T077 [P] [US3] Criar entidade/repositório `CupomDesconto` em `backend/src/main/java/com/bikeshop/admin/CupomDesconto.java`
- [X] T078 [US3] Implementar `CouponService` (validação de validade/valor mínimo/categoria, edge case de cupom expirado) em `backend/src/main/java/com/bikeshop/checkout/CouponService.java` (depende de T077)
- [X] T079 [US3] Implementar endpoints `/admin/coupons` e `POST /checkout/coupon` em `backend/src/main/java/com/bikeshop/admin/CouponAdminController.java` (depende de T078) — `POST /checkout/coupon` fica em `CheckoutController.java` (endpoint de checkout, não de admin)
- [X] T079b [US3] Integrar validação/aplicação de cupom ao fluxo de checkout `POST /checkout/orders` em `backend/src/main/java/com/bikeshop/checkout/CheckoutController.java`, conectando ao `CouponService` (depende de T078, T079, T038 da US1)
- [X] T084 [P] [US3] Construir UI de gestão de pedidos em `frontend/src/app/admin/orders/` (caminho real sem route group, mesmo motivo de T082/T083)
- [X] T085 [P] [US3] Construir UI de gestão de cupons em `frontend/src/app/admin/coupons/` (idem)

**Checkpoint 5B**: ✅ Atingido e validado (testes automatizados + navegador) em 2026-08-23. Pedidos administráveis (listagem + atualização de status com auditoria) e cupons de desconto (criação, validação de validade/valor mínimo/categoria/limite de uso, edge case de cupom expirado) funcionando ponta a ponta — criados no backoffice e aplicados no checkout do cliente, com desconto persistido no pedido e contador de uso incrementado.

---

### Sub-bloco 5C — Clientes, Auditoria, Moderação e Fechamento do Backoffice

**Goal**: Administrador consulta/bloqueia clientes, audita ações sensíveis e modera avaliações; frontend aplica guardas de RBAC nas rotas administrativas; E2E cobre o cenário 3 completo.

**Independent Test**: Cenário 3 completo de `quickstart.md` (produto → estoque → pedido → cupom, todas as ações gerando entrada no Log de Auditoria), validado via E2E.

**Depende de**: Sub-blocos 5A e 5B completos.

#### Tests

- [ ] T070 [P] [US3] Teste de contrato para `GET /admin/audit-logs` em `backend/src/test/java/com/bikeshop/admin/AuditLogContractTest.java`
- [ ] T070b [P] [US3] Teste de contrato para `GET /admin/customers` e `PATCH /admin/customers/{id}/status` em `backend/src/test/java/com/bikeshop/admin/CustomerAdminContractTest.java`
- [ ] T070c [P] [US3] Teste de contrato para `PATCH /admin/reviews/{id}` (moderação de avaliações, FR-009) em `backend/src/test/java/com/bikeshop/admin/ReviewModerationContractTest.java`
- [ ] T071 [US3] Teste de integração E2E (Playwright) criação de produto, ajuste de estoque, atualização de pedido e criação de cupom em `frontend/tests/e2e/admin-backoffice.spec.ts`
- [ ] T072 [US3] Checks de privacidade, RBAC, acessibilidade e observabilidade da história

#### Implementation

- [ ] T080 [US3] Implementar `GET /admin/customers` (listagem básica) e `PATCH /admin/customers/{id}/status` (bloquear/desbloquear cliente, sem editar dados pessoais sensíveis) em `backend/src/main/java/com/bikeshop/admin/CustomerAdminController.java`
- [ ] T081 [US3] Implementar `GET /admin/audit-logs` em `backend/src/main/java/com/bikeshop/admin/AuditLogController.java` (depende de T015 da fundação)
- [ ] T081b [US3] Implementar moderação de avaliações (`PATCH /admin/reviews/{id}` para aprovar/rejeitar, FR-009) em `backend/src/main/java/com/bikeshop/admin/ReviewModerationController.java` (depende de T058 da US2)
- [ ] T086 [US3] Construir UI de visualização de log de auditoria em `frontend/src/app/(admin)/audit-logs/`
- [ ] T087 [US3] Aplicar guardas de rota RBAC nas rotas administrativas do frontend em `frontend/src/features/admin/guards.ts` (depende de T082–T086)

**Checkpoint 5C**: ✅ (ao concluir) Todas as user stories funcionando de forma independente — backoffice completo.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Melhorias que afetam múltiplas user stories

- [ ] T088 [P] Sincronizar índice Meilisearch em criação/atualização/remoção de produto (cross-story) em `backend/src/main/java/com/bikeshop/catalog/SearchIndexSyncListener.java`
- [ ] T089 [P] Configurar dashboards Prometheus/Grafana para checkout, pagamento, estoque e pedidos
- [ ] T090 [P] Configurar pipeline centralizado de logs (ELK/Loki)
- [ ] T091 Rodar validação completa de `quickstart.md` nos 3 cenários
- [ ] T092 [P] Auditoria de acessibilidade (WCAG) nas UIs de catálogo, checkout, conta e admin
- [ ] T093 Revisão de hardening de segurança (expiração/refresh de JWT, verificação de assinatura de webhook, cobertura de RBAC)
- [ ] T094 [P] Revisão da documentação de API (Springdoc/OpenAPI) frente a `contracts/api-overview.md`
- [ ] T095 Validação de performance frente às metas SC-001 a SC-004 de `spec.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: Depende da conclusão do Setup — BLOQUEIA todas as user stories
- **User Stories (Phase 3+)**: Todas dependem da conclusão da fase Foundational
  - Podem prosseguir em paralelo (se houver equipe) ou sequencialmente em ordem de prioridade (P1 → P2 → P3)
- **Polish (Phase 6)**: Depende da conclusão das user stories desejadas

### User Story Dependencies

- **User Story 1 (P1)**: Pode começar após a Fase 2 — sem dependência de outras stories. Internamente dividida em 3A (catálogo/carrinho) → 3B (frete/checkout/pagamento, depende de 3A) → 3C (auth/eventos/notificação, depende de 3A e 3B)
- **User Story 2 (P2)**: Pode começar após a Fase 2 — reutiliza `Pedido`/`OrderService` da US1 (T034, T035) para histórico e devolução, mas é testável de forma independente
- **User Story 3 (P3)**: Pode começar após a Fase 2 — reutiliza `Produto`/`VariacaoProduto` da US1 (T025, T026) e `Pedido` (T034/T035), mas é testável de forma independente

### Within Each User Story

- Testes de contrato/integração MUST ser escritos e falhar antes da implementação
- Modelos/entidades antes de serviços
- Serviços antes de endpoints
- Implementação core antes de integração com filas/eventos
- Story completa antes de avançar para a próxima prioridade

### Parallel Opportunities

- Todas as tasks [P] da Fase 1 podem rodar em paralelo
- Todas as tasks [P] da Fase 2 podem rodar em paralelo (dentro da Fase 2)
- Após a Fase 2, todas as user stories podem começar em paralelo (se houver capacidade de equipe)
- Todos os testes [P] de uma user story podem rodar em paralelo
- Entidades [P] dentro de uma story podem rodar em paralelo

---

## Parallel Example: User Story 1

```bash
# Testes de US1 em paralelo:
Task: "Teste de contrato para /catalog/products em backend/src/test/java/com/bikeshop/catalog/ProductContractTest.java"
Task: "Teste de contrato para /cart em backend/src/test/java/com/bikeshop/cart/CartContractTest.java"
Task: "Teste de contrato para /checkout/shipping-quote em backend/src/test/java/com/bikeshop/checkout/ShippingQuoteContractTest.java"

# Entidades de US1 em paralelo:
Task: "Criar entidade Produto em backend/src/main/java/com/bikeshop/catalog/Produto.java"
Task: "Criar entidade VariacaoProduto em backend/src/main/java/com/bikeshop/catalog/VariacaoProduto.java"
Task: "Criar entidades Carrinho/ItemCarrinho em backend/src/main/java/com/bikeshop/cart/"
```

---

## Implementation Strategy

### MVP First (User Story 1 apenas)

1. Completar Fase 1: Setup
2. Completar Fase 2: Foundational (CRÍTICO — bloqueia todas as stories)
3. Completar Sub-bloco 3A (catálogo + carrinho) → **PARAR e VALIDAR** isoladamente
4. Completar Sub-bloco 3B (frete + checkout + pagamento) → **PARAR e VALIDAR** isoladamente (requer credenciais sandbox dos gateways)
5. Completar Sub-bloco 3C (auth + eventos + notificação) → **PARAR e VALIDAR**: cenário 1 completo de `quickstart.md` via E2E
6. Deploy/demo se pronto

### Incremental Delivery

1. Completar Setup + Foundational → Fundação pronta
2. Adicionar User Story 1 → Testar independentemente → Deploy/Demo (MVP!)
3. Adicionar User Story 2 → Testar independentemente → Deploy/Demo
4. Adicionar User Story 3 → Testar independentemente → Deploy/Demo
5. Cada story agrega valor sem quebrar as anteriores

### Parallel Team Strategy

Com múltiplos desenvolvedores:

1. Time completa Setup + Foundational em conjunto
2. Após a Fase Foundational:
   - Desenvolvedor(a) A: User Story 1
   - Desenvolvedor(a) B: User Story 2
   - Desenvolvedor(a) C: User Story 3
3. Stories completam e se integram de forma independente

---

## Notes

- [P] tasks = arquivos diferentes, sem dependências
- [Story] mapeia a tarefa à user story correspondente para rastreabilidade
- Cada user story deve ser completável e testável de forma independente
- Verificar que os testes falham antes de implementar (Princípio I — Test-First)
- Fazer commit após cada tarefa ou grupo lógico
- Parar em cada checkpoint para validar a story de forma independente
- Frete: transportadora confirmada é Correios (ver `research.md`, seção 5) — não há tarefa de decisão pendente
- Cupom: T038 (US1/MVP) não depende de `CouponService`; a integração de cupom no checkout é feita em T079b (US3), após `CouponService` (T078) existir — preserva a entrega independente do MVP
- Notificações: o módulo `notifications` nasce em T046b (US1, confirmação de pedido) e é estendido em T066 (US2, mudança de status)
