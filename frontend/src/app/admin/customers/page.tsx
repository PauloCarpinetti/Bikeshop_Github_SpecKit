"use client";

import { useCallback, useEffect, useState } from "react";
import { useRequireAdmin } from "@/features/admin/guards";
import { AdminNav } from "@/features/admin/AdminNav";
import { ApiRequestError } from "@/services/apiClient";
import { listCustomers, updateCustomerStatus, type Customer } from "@/services/admin";

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("pt-BR");
}

export default function AdminCustomersPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [customers, setCustomers] = useState<Customer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);

  const loadCustomers = useCallback(() => {
    listCustomers()
      .then(setCustomers)
      .catch(() => setError("Não foi possível carregar os clientes agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadCustomers();
  }, [isAuthorized, loadCustomers]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-4xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  async function handleToggleStatus(customer: Customer) {
    setSavingId(customer.id);
    setError(null);
    try {
      await updateCustomerStatus(customer.id, !customer.bloqueado);
      loadCustomers();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível atualizar o cliente agora.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Clientes (backoffice)</h1>
        <AdminNav current="/admin/customers" />
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <ul className="mt-6 space-y-2">
        {customers === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {customers?.length === 0 && <p className="text-sm text-gray-500">Nenhum cliente cadastrado ainda.</p>}
        {customers?.map((customer) => (
          <li key={customer.id} className="flex items-center justify-between rounded border p-3 text-sm">
            <div>
              <p className="font-medium">
                {customer.nome} — {customer.email}
                {customer.bloqueado && <span className="ml-2 text-xs text-red-600">(bloqueado)</span>}
              </p>
              <p className="text-xs text-gray-500">
                {customer.telefone ?? "sem telefone"} · cadastrado em {formatDate(customer.criadoEm)}
              </p>
            </div>
            <button
              type="button"
              onClick={() => handleToggleStatus(customer)}
              disabled={savingId === customer.id}
              className={`rounded px-3 py-1 text-xs font-medium text-white disabled:bg-gray-300 ${
                customer.bloqueado ? "bg-gray-900 hover:bg-gray-700" : "bg-red-600 hover:bg-red-700"
              }`}
            >
              {savingId === customer.id ? "Salvando..." : customer.bloqueado ? "Desbloquear" : "Bloquear"}
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}
