# Phase 0 Research: Plataforma E-commerce de Bicicletas e Acessórios

## 1. Frontend: Next.js + TypeScript + Tailwind CSS + React Hook Form/Zod

- **Decision**: Next.js (App Router) com React e TypeScript; Tailwind CSS para estilos; React Hook Form + Zod para formulários e validação de esquema compartilhado entre client e server.
- **Rationale**: SSR/SSG do Next.js favorece SEO de catálogo público e performance mobile (Princípio VI — Simplicidade e Acessibilidade); Zod permite validação declarativa reaproveitável nas chamadas à API; Tailwind acelera a construção de UI consistente e acessível.
- **Alternatives considered**: SPA pura (Vite/CRA) — descartada por exigir solução própria de SEO/SSR para o catálogo público; styled-components — mantido como alternativa válida ao Tailwind conforme TechStack.pdf, decisão final de estilo fica a critério do time no início da implementação.

## 2. Backend: Java + Spring Boot

- **Decision**: Spring Boot 3.x (Java 21 LTS) expondo API REST, com Spring Security + JWT para autenticação/autorização baseada em papéis, Spring Data JPA + Hibernate para persistência, MapStruct para mapeamento DTO↔entidade, Flyway para versionamento de schema MySQL, Springdoc OpenAPI/Swagger para documentação de contratos.
- **Rationale**: Ecossistema maduro para RBAC (Princípio III), transações ACID (checkout/pedido/estoque) e observabilidade via Actuator + Micrometer (Princípio V); Flyway garante migrações auditáveis e reproduzíveis.
- **Alternatives considered**: Node.js/NestJS — descartado por não constar no TechStack.pdf de referência do projeto.

## 3. Banco de dados: MySQL + Redis

- **Decision**: MySQL como armazenamento transacional primário; Redis para cache de leitura (catálogo, sessões) e carrinho de visitante com TTL.
- **Rationale**: Consistência transacional forte para pedidos/estoque; Redis reduz latência de leitura repetida e permite expirar carrinhos de visitante sem poluir o banco relacional.
- **Alternatives considered**: PostgreSQL — não descartado tecnicamente, porém fora do escopo definido em TechStack.pdf.

## 4. Pagamentos: Stripe, Mercado Pago e PagSeguro

- **Decision**: Camada de abstração `payments` no backend com um adapter por gateway (Strategy pattern), permitindo checkout selecionar o provedor por método de pagamento (cartão via Stripe, PIX/boleto via Mercado Pago ou PagSeguro). Webhooks processados de forma idempotente (chave de idempotência por evento) e persistidos antes do processamento, com reprocessamento via fila (RabbitMQ) em caso de falha.
- **Rationale**: Atende FR-006 (PIX, boleto, cartão) sem acoplar o domínio de pedidos a um gateway específico; tokenização client-side evita que dados de cartão cheguem ao backend (Princípio II).
- **Alternatives considered**: Gateway único — descartado porque a spec e o TechStack.pdf exigem suporte a três provedores para cobrir os métodos de pagamento locais (PIX/boleto) e internacionais (cartão).

## 5. Frete e transportadoras — CONFIRMADO: Correios

- **Decision**: Integração com a API dos Correios (cálculo de preço e prazo por CEP, peso e dimensões) como transportadora oficial para o cálculo de frete. Implementada como uma interface `ShippingProvider` no módulo `checkout`, com o adapter `CorreiosShippingProvider` como única implementação inicial. A interface permanece como ponto de extensão caso o negócio decida futuramente adicionar agregadores (ex.: Melhor Envio) ou transportadoras adicionais.
- **Rationale**: FR-005 exige frete dinâmico por CEP com cubagem e peso volumétrico (bicicletas montadas/semimontadas); os Correios cobrem esse cálculo nativamente e são a opção confirmada pelo negócio em 2026-07-10.
- **Confirmado em**: 2026-07-10, pelo responsável do projeto.

## 6. Busca e catálogo: Meilisearch

- **Decision**: Índice Meilisearch sincronizado a partir do MySQL (via evento RabbitMQ ao criar/atualizar produto) para busca por texto livre e filtros facetados (categoria, preço, modalidade, marca, tamanho).
- **Rationale**: Atende FR-003 com baixa latência e configuração simples de facetas, sem a complexidade operacional de Elasticsearch.
- **Alternatives considered**: Busca via `LIKE`/full-text no MySQL — descartada por não suportar facetas de forma performática.

## 7. Armazenamento de mídia: Google Cloud Storage + Cloud CDN

- **Decision**: Imagens de produto armazenadas em GCS, servidas via Cloud CDN; upload realizado pelo backoffice (admin) com validação de tipo/tamanho antes do envio.
- **Rationale**: Atende FR-002 (fotos em alta resolução) com custo e escalabilidade adequados; CDN garante carregamento rápido no catálogo (Princípio VI).

## 8. Mensageria assíncrona: RabbitMQ

- **Decision**: Filas dedicadas por domínio de evento — `orders.events`, `inventory.events`, `notifications.events` — consumidas pelos módulos `orders`, `admin` (estoque) e `notifications`.
- **Rationale**: Desacopla efeitos colaterais (notificação, atualização de índice de busca, ajuste de estoque) do caminho crítico de checkout, suportando os requisitos de confiabilidade (Princípio V) e o ciclo de vida de pedidos (FR-007).

## 9. Notificações: SendGrid, Firebase Cloud Messaging, Twilio

- **Decision**: Módulo `notifications` consome eventos da fila e delega para SendGrid (e-mail transacional), FCM (push) e, opcionalmente, Twilio (SMS/WhatsApp) conforme preferência do cliente.
- **Rationale**: Cobre confirmações de pedido, atualização de status e recuperação de checkout (SC-002) sem acoplar o fluxo de pedido ao provedor de notificação.

## 10. Infraestrutura, CI/CD e observabilidade

- **Decision**: Docker + Docker Compose para ambiente local; Kubernetes (GKE) opcional para produção; Nginx como proxy reverso; GitHub Actions para CI/CD (build, test, lint, deploy); Prometheus + Grafana para métricas, ELK Stack ou Loki para logs centralizados, Sentry para rastreamento de erros de frontend e backend.
- **Rationale**: Cobre diretamente o Princípio V (Observabilidade e Confiabilidade) e permite pipelines reproduzíveis alinhados ao workflow Scrum da constituição (revisão e teste antes do merge).

## 11. Testes

- **Decision**: JUnit + Mockito para testes unitários/integração do backend; Jest + React Testing Library para componentes do frontend; Playwright para os fluxos E2E críticos (catálogo→checkout, conta/pós-venda, backoffice), mapeados diretamente aos cenários de aceitação das três user stories da spec.
- **Rationale**: Atende ao Princípio I (Test-First, não negociável), garantindo que cada história tenha cobertura automatizada correspondente antes do merge.
