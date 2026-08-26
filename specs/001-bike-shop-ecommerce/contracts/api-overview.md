# API Contracts (Overview): Plataforma E-commerce de Bicicletas e Acessórios

Contrato REST exposto pelo backend Spring Boot, documentado formalmente via Springdoc OpenAPI/Swagger (`/v3/api-docs`, `/swagger-ui.html`). Este documento lista a superfície de endpoints por módulo, para orientar o desenho do OpenAPI real e a quebra em tarefas. Não substitui o `openapi.yaml` gerado a partir das anotações do código durante a implementação.

## Convenções

- Base path: `/api/v1`
- Autenticação: `Authorization: Bearer <JWT>` (Spring Security); endpoints públicos de catálogo não exigem token
- Erros: corpo padrão `{ code, message, details? }` com status HTTP apropriado
- Paginação: `page`, `size`, `sort` como query params em listagens

## Catálogo (`/catalog`) — público

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| GET | `/products` | Lista produtos com busca (`q`) e filtros facetados (categoria, preço, marca, tamanho, modalidade) | FR-003 |
| GET | `/products/{slug}` | Detalhe do produto: especificações, geometria, imagens e variações (SKU, atributos, preço, disponibilidade) já embutidas na resposta — não há endpoint separado para variações | FR-001, FR-002 |

## Carrinho (`/cart`) — visitante ou autenticado

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| GET | `/cart` | Recupera carrinho atual (por sessão ou cliente) | FR-004 |
| POST | `/cart/items` | Adiciona item (SKU + quantidade) | FR-004 |
| PATCH | `/cart/items/{itemId}` | Atualiza quantidade | FR-004 |
| DELETE | `/cart/items/{itemId}` | Remove item | FR-004 |

O merge do carrinho de visitante ao carrinho do cliente não é uma rota própria — acontece internamente em `POST /auth/register` e `POST /auth/login` (ver seção Autenticação).

## Checkout (`/checkout`)

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| POST | `/checkout/shipping-quote` | Calcula frete por CEP para os itens do carrinho | FR-005 |
| POST | `/checkout/coupon` | Aplica/valida cupom de desconto no carrinho | FR-009 (backoffice cria; checkout consome) |
| POST | `/checkout/orders` | Cria pedido a partir do carrinho, endereço e método de pagamento | FR-006, FR-007 |

## Pagamentos (`/payments`)

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| POST | `/payments/{orderId}/intents` | Cria intenção/sessão de pagamento no gateway selecionado (Stripe/Mercado Pago/PagSeguro) | FR-006 |
| POST | `/payments/webhooks/{provider}` | Recebe e processa webhooks de forma idempotente | FR-006 |

## Conta do cliente (`/account`) — autenticado

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| GET/PUT | `/account/profile` | Consulta/atualiza dados cadastrais | FR-008 |
| GET/POST/PUT | `/account/addresses` | Gerencia endereços | FR-008 |
| GET | `/account/orders` | Histórico de pedidos do cliente | FR-008 |
| GET | `/account/orders/{orderId}` | Detalhe e rastreamento do pedido | FR-008 |
| POST | `/account/orders/{orderId}/return` | Solicita troca/devolução | FR-008 |
| POST | `/account/reviews` | Publica avaliação de produto entregue | FR-008 |

## Autenticação (`/auth`)

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| POST | `/auth/register` | Cadastro de cliente; mescla o carrinho de visitante da sessão atual ao carrinho do cliente | FR-004, FR-008 |
| POST | `/auth/login` | Login, emite JWT; mescla o carrinho de visitante ao carrinho do cliente | FR-004, FR-008 |
| POST | `/auth/refresh` | Renova token (recusa se o refresh token estiver revogado) | — |
| POST | `/auth/logout` | Revoga o access token da requisição e, se informado, o refresh token — via blacklist no Redis (T093) | — |

## Backoffice (`/admin`) — papéis administrativo/operacional

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| GET/POST/PUT/DELETE | `/admin/products` | CRUD de produtos (`GET` lista e `GET /{id}` detalha) | FR-001, FR-009 |
| POST/PUT | `/admin/products/{id}/variants` | Adiciona/atualiza variação de um produto | FR-001, FR-009 |
| PATCH | `/admin/products/{sku}/stock` | Ajuste manual de estoque | FR-009 |
| GET/PATCH | `/admin/orders` | Lista e atualiza status de pedidos, emite documentos de envio | FR-007, FR-009 |
| GET/POST/PUT/DELETE | `/admin/coupons` | CRUD de cupons de desconto (`GET` lista) | FR-009 |
| GET | `/admin/customers` | Consulta básica de clientes | FR-009 |
| PATCH | `/admin/customers/{id}/status` | Bloqueia/desbloqueia cliente (sem editar dados pessoais sensíveis) | FR-009 |
| GET/PATCH | `/admin/reviews` | Lista avaliações para moderação (`GET`) e aprova/rejeita (`PATCH /{id}`) | FR-009 |
| GET | `/admin/audit-logs` | Consulta de logs de auditoria | FR-011 |

Todas as rotas sob `/admin` exigem papel com privilégio adequado (RBAC, Princípio III) e geram entrada no Log de Auditoria para operações de escrita (FR-011).
