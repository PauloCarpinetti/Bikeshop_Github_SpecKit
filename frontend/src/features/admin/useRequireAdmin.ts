"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";

const ADMIN_ROLES = ["ADMIN", "OPERATOR"];

/**
 * Guarda mínima de RBAC para páginas do backoffice (T087 formaliza isso em
 * frontend/src/features/admin/guards.ts na Fase 5C) — redireciona quem não é ADMIN/OPERATOR.
 * O backend já recusa (403) as chamadas de API mesmo sem isso; a guarda evita expor a tela em si.
 */
export function useRequireAdmin() {
  const router = useRouter();
  const { cliente, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!cliente || !ADMIN_ROLES.includes(cliente.role)) {
      router.push("/login");
    }
  }, [isLoading, cliente, router]);

  const isAuthorized = !isLoading && !!cliente && ADMIN_ROLES.includes(cliente.role);
  return { isAuthorized, isCheckingAuth: isLoading };
}
