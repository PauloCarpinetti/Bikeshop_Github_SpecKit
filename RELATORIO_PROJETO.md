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

## Estado atual do projeto

| Item | Status |
|---|---|
| Constituição | ✅ Definida |
| Especificação (spec.md) | ✅ Completa, revisada e corrigida |
| Plano técnico (plan.md + research/data-model/quickstart/contracts) | ✅ Completo |
| Backlog de tarefas (tasks.md) | ✅ 101 tarefas, consistência validada — **56 concluídas** |
| Fase 1 — Setup | ✅ Implementada e validada |
| Fase 2 — Foundational | ✅ Implementada e validada |
| Fase 3A — Catálogo e Carrinho | ✅ Implementada e validada |
| Fase 3B — Frete, Checkout e Pagamento | ✅ Implementada e validada (pagamento/frete reais pendentes de credenciais) |
| Fase 3C — Autenticação e fechamento do MVP | ✅ Implementada e validada (**MVP / User Story 1 completo**) |
| Fase 4A — Perfil, Endereços e Histórico de Pedidos | ✅ Implementada e validada |
| Fase 4B — Avaliações, Trocas/Devoluções e Notificação de Status | ⏳ Não iniciada |
| Fase 5 — User Story 3 (backoffice) | ⏳ Não iniciada |
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

### 2. Sub-bloco 4B e Fase 5

Avaliações de produto, trocas/devoluções e notificação de mudança de status (fecha a User Story 2), e backoffice completo (User Story 3), já detalhadas em `tasks.md`.

### 3. Fase 6 — Polish

Dashboards de observabilidade, logs centralizados, auditoria de acessibilidade, hardening de segurança e validação final de performance contra os critérios de sucesso do `spec.md`.

## Resumo executivo

O MVP (User Story 1) está completo: catálogo com busca, carrinho, cálculo de frete por peso real, checkout completo com pagamento (simulado, pronto para credenciais reais), autenticação (cadastro/login/JWT) com merge de carrinho de visitante, e evento de pedido disparando notificação de confirmação (simulada) — tudo testado via testes automatizados e navegador. Restam conta do cliente, backoffice e os itens de polimento — todos já detalhados em `tasks.md`.
