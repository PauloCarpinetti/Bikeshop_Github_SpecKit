"use client";

import Link from "next/link";

const LINKS = [
  { href: "/admin/products", label: "Produtos" },
  { href: "/admin/inventory", label: "Estoque" },
  { href: "/admin/orders", label: "Pedidos" },
  { href: "/admin/coupons", label: "Cupons" },
  { href: "/admin/customers", label: "Clientes" },
  { href: "/admin/reviews", label: "Avaliações" },
  { href: "/admin/audit-logs", label: "Auditoria" },
];

/**
 * Navegação entre as telas do backoffice, compartilhada por todas elas (consolidada junto com
 * `guards.ts` na Fase 5C — antes cada página listava manualmente só os links para as demais).
 */
export function AdminNav({ current }: { current: string }) {
  return (
    <nav className="flex flex-wrap gap-3 text-sm">
      {LINKS.filter((link) => link.href !== current).map((link) => (
        <Link key={link.href} href={link.href} className="underline">
          {link.label}
        </Link>
      ))}
    </nav>
  );
}
