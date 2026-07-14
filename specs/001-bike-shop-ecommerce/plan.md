# Implementation Plan: Plataforma E-commerce de Bicicletas e Acessórios

**Branch**: `001-bike-shop-ecommerce` | **Date**: 2026-07-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-bike-shop-ecommerce/spec.md`

## Summary

Construir uma plataforma de e-commerce B2C para bicicletas e acessórios cobrindo três jornadas prioritárias: (P1) descoberta de catálogo, carrinho e checkout para visitantes e clientes; (P2) conta do cliente, histórico de pedidos e pós-venda; (P3) backoffice de catálogo, estoque, pedidos e promoções com controle de acesso baseado em papéis.

Abordagem técnica: aplicação web com frontend em Next.js (React + TypeScript) consumindo uma API REST em Java/Spring Boot, com MySQL como armazenamento transacional, Redis para cache e sessões, Meilisearch para busca facetada de catálogo, RabbitMQ para eventos assíncronos (pedidos, estoque, notificações), e integrações externas para pagamento (Stripe, Mercado Pago, PagSeguro), armazenamento de mídia (Google Cloud Storage + Cloud CDN) e notificações transacionais (SendGrid, Firebase Cloud Messaging, Twilio). Infraestrutura em contêineres (Docker/Kubernetes) sobre Google Cloud Platform, com observabilidade via Prometheus/Grafana, logs centralizados e Sentry, conforme os princípios de Observabilidade, RBAC e Privacidade da constituição do projeto.

## Technical Context

**Language/Version**: TypeScript (frontend, Next.js/React 18+) e Java 21 LTS (backend, Spring Boot 3.x)

**Primary Dependencies**:
- Frontend: Next.js (React), Tailwind CSS (ou styled-components), React Hook Form + Zod (formulários e validação)
- Backend: Spring Boot (REST API), Spring Security + JWT (autenticação/autorização), Spring Data JPA + Hibernate (persistência), MapStruct (mapeamento DTO/entidade), Flyway (versionamento de schema), Springdoc OpenAPI/Swagger (documentação de API)

**Storage**: MySQL (dados transacionais: produtos, variações, pedidos, clientes, cupons, avaliações, logs de auditoria) + Redis (cache de catálogo/sessões e carrinho de visitante)

**Testing**: JUnit + Mockito (backend, unitário e integração), Jest + React Testing Library (frontend, unitário e componente), Playwright (testes end-to-end dos fluxos críticos: catálogo→checkout, conta/pós-venda, backoffice)

**Target Platform**: Aplicação web responsiva (mobile-first) implantada em Google Cloud Platform (GKE e/ou Cloud Run), atrás de Nginx, com contêineres Docker orquestrados via Docker Compose (dev) e Kubernetes (produção, opcional conforme escala)

**Project Type**: Web application (frontend + backend separados)

**Performance Goals**:
- Descoberta → checkout iniciado em menos de 3 minutos numa jornada típica (SC-001)
- Busca e filtros facetados (Meilisearch) respondendo em menos de 300ms para catálogos de porte médio
- Páginas de catálogo e produto em mobile dentro das metas "boas" de Core Web Vitals: LCP < 2.5s, INP < 200ms, CLS < 0.1
- Processamento assíncrono de eventos de pedido/estoque via RabbitMQ sem bloquear a resposta ao usuário

**Constraints**:
- Nenhum dado sensível de cartão deve trafegar ou ser persistido diretamente pela aplicação — pagamentos via tokenização/redirect dos gateways (Stripe, Mercado Pago, PagSeguro), conforme Privacidade & Proteção de Dados (Princípio II)
- Toda ação administrativa/operacional sensível deve ser auditável e restrita por papel (Princípio III), refletido no Log de Auditoria (spec.md)
- Fluxos críticos (checkout, pagamento, estoque, pedidos) MUST emitir eventos observáveis (Prometheus/Grafana, ELK/Loki, Sentry), conforme Princípio V
- Interfaces MUST permanecer simples e acessíveis, com foco mobile (Princípio VI)
- Webhooks de pagamento devem ser idempotentes e reconciliáveis (retentativa/backoff), dado que FR-006 exige atualização automática via webhook

**Scale/Scope**: MVP B2C com catálogo público, busca/filtros, carrinho, checkout, autenticação de cliente e painel administrativo inicial (catálogo, estoque, pedidos, cupons); volume inicial dimensionado para um único lojista/catálogo (sem marketplace multi-loja ou múltiplos armazéns, conforme premissas da spec)

**Transportadora de frete**: Correios (API oficial de cálculo por CEP/peso/dimensões), confirmado em 2026-07-10. Ver `research.md` (seção 5).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| **I. Test-First (NON-NEGOTIABLE)** | PASS — Stack de testes definida por camada (JUnit/Mockito, Jest/RTL, Playwright); cada user story da spec já tem cenários Given/When/Then utilizáveis como testes de aceitação antes da implementação. |
| **II. Privacidade e Proteção de Dados** | PASS — Dados de cartão nunca tocam a aplicação (gateways com tokenização); MySQL/Redis sob controle próprio, sem terceiros adicionais para dados pessoais além dos gateways de pagamento/notificação já listados. |
| **III. RBAC** | PASS — Spring Security + JWT permite papéis (cliente, operador, administrador); Log de Auditoria (spec.md) cobre rastreabilidade de ações privilegiadas. |
| **IV. Modularidade por Feature** | PASS — Estrutura de projeto (abaixo) organiza o backend em módulos por domínio (catálogo, carrinho, pedidos, pagamentos, contas, backoffice, auditoria) com fronteiras claras. |
| **V. Observabilidade e Confiabilidade** | PASS — Prometheus/Grafana, ELK/Loki e Sentry cobrem métricas, logs e erros; RabbitMQ isola falhas assíncronas de estoque/notificação do fluxo síncrono de checkout. |
| **VI. Simplicidade e Acessibilidade** | PASS — Next.js + Tailwind favorece componentes simples e acessíveis; nenhuma complexidade adicional (ex.: microsserviços) introduzida além do necessário para separar frontend/backend. |

Nenhuma violação identificada. Ver `Complexity Tracking` (vazio).

## Project Structure

### Documentation (this feature)

```text
specs/001-bike-shop-ecommerce/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/bikeshop/
│   ├── catalog/          # Produto, Variação de Produto, busca (integração Meilisearch)
│   ├── cart/              # Carrinho (visitante e autenticado), merge de carrinho
│   ├── checkout/          # Orquestração de checkout, frete, cupom
│   ├── orders/            # Pedido, ciclo de vida/status, histórico
│   ├── payments/          # Integração Stripe / Mercado Pago / PagSeguro, webhooks
│   ├── customers/         # Cliente, endereços, autenticação (Spring Security + JWT)
│   ├── reviews/           # Avaliação de produtos
│   ├── admin/             # Backoffice: CRUD produtos, estoque, pedidos, cupons
│   ├── audit/             # Log de Auditoria (ações administrativas sensíveis)
│   ├── notifications/     # Integração SendGrid, FCM, Twilio (consumidores RabbitMQ)
│   └── common/            # Config Spring Security, MapStruct, exception handling, OpenAPI
├── src/main/resources/
│   └── db/migration/      # Scripts Flyway (versionamento MySQL)
└── src/test/java/com/bikeshop/
    ├── unit/
    ├── integration/
    └── contract/

frontend/
├── src/app/                # Rotas Next.js (App Router): catálogo, produto, carrinho, checkout, conta, admin
├── src/components/          # Componentes de UI reutilizáveis (acessíveis, Tailwind)
├── src/features/            # Lógica por feature (catálogo, carrinho, checkout, conta, admin)
├── src/services/            # Clientes de API (fetch/axios), schemas Zod
└── tests/
    ├── unit/                 # Jest + React Testing Library
    └── e2e/                  # Playwright

infra/
├── docker-compose.yml        # Ambiente local: backend, frontend, MySQL, Redis, RabbitMQ, Meilisearch
├── k8s/                       # Manifests Kubernetes (GKE) — opcional conforme escala
└── nginx/                     # Configuração de proxy reverso

.github/workflows/            # Pipelines CI/CD (build, test, lint, deploy)
```

**Structure Decision**: Aplicação web com frontend e backend desacoplados (Option 2: Web application). O backend Spring Boot é organizado em módulos por domínio de negócio (catálogo, carrinho, pedidos, pagamentos, clientes, avaliações, admin, auditoria, notificações), alinhado ao Princípio IV (Modularidade por Feature). O frontend Next.js segue a mesma divisão por feature em `src/features/`, evitando acoplamento cruzado. Integrações externas (pagamento, storage, mensageria, notificações) ficam isoladas em módulos/adapters dedicados no backend para permitir substituição futura sem afetar o domínio.

## Complexity Tracking

*Nenhuma violação da constituição identificada nesta fase — tabela não preenchida.*
