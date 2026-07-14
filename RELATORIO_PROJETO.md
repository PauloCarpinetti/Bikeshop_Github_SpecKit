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

Fase 3 tem 31 tarefas (catálogo+busca, carrinho, frete, checkout, 3 gateways de pagamento, pedidos, autenticação, notificações, 6 páginas de frontend) — bem mais complexa que a Fase 2. Estimativa discutida: **3–5 horas de trabalho**, possivelmente em 2 sessões, mais o tempo para o usuário providenciar credenciais sandbox do Stripe, Mercado Pago e PagSeguro (bloqueio externo necessário para validar o checkout de ponta a ponta). Decidido **adiar o início da Fase 3** para planejá-la de forma mais assertiva numa próxima sessão.

---

## Estado atual do projeto

| Item | Status |
|---|---|
| Constituição | ✅ Definida |
| Especificação (spec.md) | ✅ Completa, revisada e corrigida |
| Plano técnico (plan.md + research/data-model/quickstart/contracts) | ✅ Completo |
| Backlog de tarefas (tasks.md) | ✅ 101 tarefas, consistência validada |
| Fase 1 — Setup | ✅ Implementada e validada |
| Fase 2 — Foundational | ✅ Implementada e validada |
| Fase 3 — User Story 1 (MVP: catálogo, carrinho, checkout) | ⏳ Não iniciada |
| Fase 4 — User Story 2 (conta, pós-venda) | ⏳ Não iniciada |
| Fase 5 — User Story 3 (backoffice) | ⏳ Não iniciada |
| Fase 6 — Polish | ⏳ Não iniciada |

## URLs de desenvolvimento local (atuais)

- Backend: `http://localhost:8081` (Swagger: `/swagger-ui/index.html`)
- Frontend: `http://localhost:3002`
- MySQL (container): porta `3307`
- Redis: `6379` · RabbitMQ: `5672` (management `15672`) · Meilisearch: `7700`

## Próximos passos recomendados

### 1. Preparar credenciais externas (fazer em paralelo, não depende de código)

- Criar contas sandbox no **Stripe**, **Mercado Pago** e **PagSeguro** e obter chaves de teste
- Confirmar se haverá conta/API real dos **Correios** para o cálculo de frete em produção (hoje usamos a decisão já confirmada, mas as credenciais de acesso ainda precisam ser levantadas)

### 2. Planejar a Fase 3 de forma mais assertiva

Antes de começar, definir:
- Se a Fase 3 será quebrada em sub-blocos (ex.: catálogo+carrinho primeiro, depois checkout+pagamento, depois auth+notificações+frontend) para reduzir risco de sessões muito longas
- Qual sub-bloco entrega o primeiro incremento testável

### 3. Implementar a Fase 3 (User Story 1 — MVP)

Catálogo, busca (Meilisearch), carrinho (visitante + autenticado), frete (Correios), checkout, pagamento (Stripe/Mercado Pago/PagSeguro), pedidos, autenticação (JWT), notificação de confirmação e tela de recuperação de checkout — 31 tarefas (T019–T047 + T044b/T046b) já detalhadas em `tasks.md`.

### 4. Seguir para as Fases 4 e 5

Conta do cliente/pós-venda (User Story 2) e backoffice completo (User Story 3), também já detalhadas em `tasks.md`.

### 5. Fase 6 — Polish

Dashboards de observabilidade, logs centralizados, auditoria de acessibilidade, hardening de segurança e validação final de performance contra os critérios de sucesso do `spec.md`.

## Resumo executivo

O projeto evoluiu da fase de descoberta (spec + constituição) para um plano técnico completo, validado por análise de consistência automatizada, e já tem uma base de código real funcionando localmente (backend Spring Boot + frontend Next.js + infraestrutura Docker), com testes passando. O próximo passo natural é a implementação da User Story 1 (MVP de catálogo e checkout), que será planejada com mais cuidado numa próxima sessão dado seu volume e as dependências externas (gateways de pagamento).
