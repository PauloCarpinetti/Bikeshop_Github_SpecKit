package com.bikeshop.common.security;

/**
 * Papéis RBAC da plataforma (Princípio III - privilégio mínimo). CUSTOMER: cliente autenticado
 * (loja, conta, pós-venda). OPERATOR: equipe operacional do backoffice (estoque, pedidos). ADMIN:
 * administração completa do backoffice (produtos, cupons, auditoria).
 */
public enum Role {
  CUSTOMER,
  OPERATOR,
  ADMIN
}
