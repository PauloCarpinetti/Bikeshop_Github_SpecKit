# Quickstart: Validação da Plataforma E-commerce de Bicicletas e Acessórios

Guia para validar, de ponta a ponta, que o ambiente sobe corretamente e que os fluxos das três user stories da spec funcionam. Detalhes de modelos/serviços/migrações ficam em `data-model.md`, `contracts/` e no `tasks.md` (gerado por `/speckit.tasks`).

## Pré-requisitos

- Docker + Docker Compose instalados
- Java 21 e Node.js LTS instalados (para rodar backend/frontend fora de contêiner, se necessário)
- Chaves de sandbox dos gateways de pagamento (Stripe, Mercado Pago, PagSeguro) configuradas como variáveis de ambiente
- Instância de projeto GCP (ou emulador local) para Google Cloud Storage, caso o upload de imagens seja testado

## Subindo o ambiente local

```bash
docker compose -f infra/docker-compose.yml up -d mysql redis rabbitmq meilisearch
```

Isso deve iniciar: MySQL (porta padrão 3306), Redis (6379), RabbitMQ (5672 + painel de gestão 15672) e Meilisearch (7700).

```bash
# Backend
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

Backend disponível em `http://localhost:8080` (Swagger UI em `/swagger-ui.html`), frontend em `http://localhost:3000`.

## Cenário 1 — Catálogo e checkout (User Story 1, P1)

1. Acessar a loja como visitante e navegar pelo catálogo aplicando ao menos um filtro facetado.
2. Abrir a página de detalhes de um produto e confirmar exibição de geometria/especificações/fotos.
3. Adicionar um item ao carrinho, informar um CEP válido e confirmar que uma estimativa de frete é exibida.
4. Prosseguir ao checkout e concluir o pedido usando um método de pagamento sandbox (PIX, boleto ou cartão).
5. Durante o checkout, autenticar-se ou criar conta e confirmar que os itens do carrinho de visitante são preservados após o login.

**Resultado esperado**: pedido criado com status inicial, evento publicado em `orders.events`, e-mail transacional de confirmação disparado (SendGrid sandbox).

## Cenário 2 — Conta, pedidos e pós-venda (User Story 2, P2)

1. Logar como cliente com pedido(s) existentes (via seed de dados de teste).
2. Atualizar endereço e senha na área de conta e confirmar persistência.
3. Acessar o histórico de pedidos e verificar status/rastreamento do pedido mais recente.
4. Solicitar troca/devolução de um item entregue e confirmar que a solicitação é registrada.
5. Publicar uma avaliação para um produto de um pedido marcado como entregue.

**Resultado esperado**: alterações refletidas imediatamente na conta; solicitação de pós-venda visível no backoffice (Cenário 3).

## Cenário 3 — Backoffice (User Story 3, P3)

1. Logar como administrador (papel `admin`) e acessar o backoffice.
2. Cadastrar um novo produto com ao menos duas variações (SKUs distintos).
3. Ajustar manualmente o estoque de uma variação e confirmar reflexo imediato na disponibilidade exibida no catálogo.
4. Alterar o status de um pedido existente e confirmar notificação/evento gerado.
5. Criar um cupom de desconto e aplicá-lo em um carrinho de teste; validar rejeição de um cupom expirado (edge case da spec).

**Resultado esperado**: todas as ações acima geram entradas no Log de Auditoria com ator, timestamp e diff da alteração.

## Verificação de observabilidade

- Métricas do backend visíveis em Prometheus/Grafana (endpoint Actuator `/actuator/prometheus`)
- Logs centralizados acessíveis via ELK/Loki
- Erros de frontend/backend capturados no Sentry durante os cenários acima (forçar um erro controlado para validar)

## Critérios de sucesso (referência a `spec.md`)

- SC-001: Cenário 1 completado (descoberta → checkout) em menos de 3 minutos
- SC-002/SC-003: Pedido chega a um status terminal claro ou mensagem de recuperação, sem intervenção manual
- SC-004: Ações do Cenário 3 completadas em menos de 10 minutos cada
- SC-005: Nenhuma falha crítica silenciosa — todo erro relevante aparece em Sentry/logs
