"use client";

import { useCallback, useEffect, useState } from "react";
import { useRequireAdmin } from "@/features/admin/guards";
import { AdminNav } from "@/features/admin/AdminNav";
import { listAuditLogs, type AuditLogEntry } from "@/services/admin";

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("pt-BR");
}

export default function AdminAuditLogsPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [logs, setLogs] = useState<AuditLogEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadLogs = useCallback(() => {
    listAuditLogs()
      .then(setLogs)
      .catch(() => setError("Não foi possível carregar o log de auditoria agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadLogs();
  }, [isAuthorized, loadLogs]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-5xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Log de auditoria (backoffice)</h1>
        <AdminNav current="/admin/audit-logs" />
      </div>
      <p className="mt-2 text-sm text-gray-500">
        Registro somente-leitura das últimas ações administrativas sensíveis (FR-011).
      </p>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <ul className="mt-6 space-y-2">
        {logs === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {logs?.length === 0 && <p className="text-sm text-gray-500">Nenhuma ação registrada ainda.</p>}
        {logs?.map((entry) => (
          <li key={entry.id} className="rounded border p-3 text-sm">
            <p className="font-medium">
              {entry.action} — {entry.entityName} #{entry.entityId}
            </p>
            <p className="text-xs text-gray-500">
              {formatDateTime(entry.occurredAt)} · por {entry.actor} ({entry.actorRole})
            </p>
            {(entry.previousState || entry.newState) && (
              <p className="mt-1 font-mono text-xs text-gray-600">
                {entry.previousState && <span>de {entry.previousState}</span>}
                {entry.previousState && entry.newState && " → "}
                {entry.newState && <span>{entry.newState}</span>}
              </p>
            )}
          </li>
        ))}
      </ul>
    </main>
  );
}
