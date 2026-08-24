"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRequireAdmin } from "@/features/admin/useRequireAdmin";
import { ApiRequestError } from "@/services/apiClient";
import { listAdminOrders, updateOrderStatus } from "@/services/admin";
import type { Order } from "@/services/checkout";

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("pt-BR");
}

const STATUS_OPTIONS = [
  "CRIADO",
  "AGUARDANDO_PAGAMENTO",
  "PAGO",
  "EM_SEPARACAO",
  "ENVIADO",
  "ENTREGUE",
  "PAGAMENTO_RECUSADO",
  "CANCELADO",
  "EM_TROCA_DEVOLUCAO",
];

export default function AdminOrdersPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingStatus, setPendingStatus] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  const loadOrders = useCallback(() => {
    listAdminOrders()
      .then(setOrders)
      .catch(() => setError("Não foi possível carregar os pedidos agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadOrders();
  }, [isAuthorized, loadOrders]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-4xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  async function handleUpdateStatus(orderId: number) {
    const status = pendingStatus[orderId];
    if (!status) return;
    setSavingId(orderId);
    setError(null);
    try {
      await updateOrderStatus(orderId, status);
      loadOrders();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível atualizar o status agora.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Pedidos (backoffice)</h1>
        <div className="flex gap-3 text-sm">
          <Link href="/admin/products" className="underline">Produtos</Link>
          <Link href="/admin/coupons" className="underline">Cupons</Link>
        </div>
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <ul className="mt-6 space-y-3">
        {orders === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {orders?.map((order) => (
          <li key={order.id} className="rounded border p-4 text-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Pedido #{order.id} — {order.clienteNome} ({order.clienteEmail})</p>
                <p className="text-xs text-gray-500">
                  {formatDate(order.criadoEm)} · {formatPrice(order.valorTotal)} · status atual: {order.status}
                  {order.cupomCodigo && ` · cupom ${order.cupomCodigo}`}
                </p>
              </div>
            </div>

            <div className="mt-3 flex items-center gap-2">
              <label htmlFor={`status-${order.id}`} className="sr-only">Novo status para o pedido #{order.id}</label>
              <select
                id={`status-${order.id}`}
                value={pendingStatus[order.id] ?? order.status}
                onChange={(e) => setPendingStatus((current) => ({ ...current, [order.id]: e.target.value }))}
                className="rounded border px-2 py-1 text-sm"
              >
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
              <button
                type="button"
                onClick={() => handleUpdateStatus(order.id)}
                disabled={savingId === order.id}
                className="rounded bg-gray-900 px-3 py-1 text-xs font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
              >
                {savingId === order.id ? "Salvando..." : "Atualizar status"}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </main>
  );
}
