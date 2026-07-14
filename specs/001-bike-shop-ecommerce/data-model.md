# Phase 1 Data Model: Plataforma E-commerce de Bicicletas e Acessórios

Entidades extraídas de `spec.md` (seção Key Entities) e dos requisitos funcionais, mapeadas para o armazenamento MySQL (transacional) com cache/sessão em Redis onde indicado.

## Produto

- **Campos**: id, nome, slug, descrição, categoria, marca, modalidade, especificações técnicas, tabela de geometria, imagens (referências GCS), status (ativo/inativo), criado_em, atualizado_em
- **Relacionamentos**: 1:N com Variação de Produto; N:1 com Categoria/Marca; indexado no Meilisearch para busca/facetas (FR-003)
- **Regras de validação**: nome e categoria obrigatórios; ao menos uma variação ativa para o produto ficar disponível para venda (FR-001)

## Variação de Produto

- **Campos**: id, produto_id, SKU (único), atributos (tamanho do quadro, cor, material, etc.), preço, estoque_disponível, estoque_reservado, status
- **Relacionamentos**: N:1 com Produto; referenciada por Item de Carrinho e Item de Pedido
- **Regras de validação**: SKU único por combinação de atributos (FR-001); estoque não pode ficar negativo; mudanças de estoque geram evento `inventory.events` (RabbitMQ)
- **Estados**: `disponível` → `esgotado` (estoque_disponível = 0) → `disponível` (reabastecimento); `descontinuado` (fim de vida)

## Cliente

- **Campos**: id, nome, e-mail (único), hash_senha, telefone, papéis (roles), endereços (1:N), preferências de notificação, criado_em
- **Relacionamentos**: 1:N com Endereço, Pedido, Avaliação; associado a um Carrinho autenticado
- **Regras de validação**: e-mail único e verificado; dados pessoais tratados conforme Princípio II (minimização e proteção)

## Carrinho

- **Campos**: id, cliente_id (nullable — visitante), sessão_id (para visitante, armazenado em Redis com TTL), itens (1:N Item de Carrinho), criado_em, expira_em
- **Relacionamentos**: N:1 com Cliente (opcional); 1:N com Item de Carrinho (referencia Variação de Produto)
- **Regras de validação**: ao autenticar/registrar, carrinho de visitante MUST ser mesclado ao carrinho do cliente (FR-004), com resolução de conflito por soma de quantidades para o mesmo SKU

## Pedido

- **Campos**: id, cliente_id, itens (1:N Item de Pedido, com preço congelado no momento da compra), endereço de entrega, frete (valor e transportadora), cupom aplicado, status, histórico de status (com timestamp e ator), pagamento (gateway, referência externa, status)
- **Relacionamentos**: N:1 com Cliente; 1:N com Item de Pedido; referencia Cupom de Desconto (opcional); gera eventos em `orders.events`
- **Estados** (FR-007): `criado` → `aguardando_pagamento` → `pago` → `em_separação` → `enviado` → `entregue`; ramificações `pagamento_recusado`, `cancelado`, `em_troca_devolução`
- **Regras de validação**: transições de status controladas (não é possível voltar de `entregue` para `criado`); todo pagamento confirmado via webhook idempotente

## Cupom de Desconto

- **Campos**: id, código (único), tipo (percentual/valor fixo), valor, validade (início/fim), valor_mínimo_carrinho, categorias_aplicáveis, limite_de_uso
- **Relacionamentos**: referenciado por Pedido (0 ou 1 por pedido)
- **Regras de validação**: cupom expirado, esgotado ou incompatível com o carrinho MUST ser rejeitado com mensagem clara (Edge Case da spec)

## Avaliação

- **Campos**: id, produto_id, cliente_id, pedido_id (para confirmar compra), nota, comentário, status (publicada/moderada), criado_em
- **Relacionamentos**: N:1 com Produto e Cliente; vinculada a um Pedido entregue
- **Regras de validação**: só pode ser criada para produtos com pedido confirmado como entregue (spec, User Story 2)

## Log de Auditoria

- **Campos**: id, ator (usuário/serviço), papel, ação, entidade_afetada, entidade_id, dados_anteriores/novos (diff), timestamp, origem (IP/contexto)
- **Relacionamentos**: registra ações sobre Produto, Variação, Pedido, Cupom, Cliente (via backoffice)
- **Regras de validação**: imutável (somente inserção); toda ação administrativa sensível MUST gerar um registro (FR-011, Princípio III)

## Endereço (subentidade de Cliente)

- **Campos**: id, cliente_id, CEP, logradouro, número, complemento, cidade, estado, tipo (entrega/cobrança), padrão (boolean)
- **Relacionamentos**: N:1 com Cliente; referenciado por Pedido no momento da compra (snapshot, não referência viva)
