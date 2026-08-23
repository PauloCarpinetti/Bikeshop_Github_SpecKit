"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";
import { listOrders } from "@/services/account";
import type { Order } from "@/services/checkout";

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
}

export default function OrdersPage() {
  const router = useRouter();
  const { cliente, isLoading: isAuthLoading } = useAuth();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthLoading && !cliente) {
      router.push("/login");
    }
  }, [isAuthLoading, cliente, router]);

  useEffect(() => {
    if (!cliente) return;
    listOrders()
      .then(setOrders)
      .catch(() => setError("Não foi possível carregar seus pedidos agora."));
  }, [cliente]);

  if (isAuthLoading || !cliente || orders === null) {
    return <p className="mx-auto max-w-3xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <h1 className="text-2xl font-semibold">Meus pedidos</h1>
      <p className="mt-1 text-sm text-gray-600"><Link href="/profile" className="underline">Voltar para minha conta</Link></p>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      {orders.length === 0 ? (
        <p className="mt-6 text-sm text-gray-500">Você ainda não fez nenhum pedido.</p>
      ) : (
        <ul className="mt-6 space-y-3">
          {orders.map((order) => (
            <li key={order.id}>
              <Link
                href={`/orders/${order.id}`}
                className="block rounded border p-4 text-sm hover:bg-gray-50"
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium">Pedido #{order.id}</span>
                  <span className="rounded bg-gray-100 px-2 py-0.5 text-xs">{order.status}</span>
                </div>
                <div className="mt-1 flex items-center justify-between text-gray-600">
                  <span>{formatDate(order.criadoEm)}</span>
                  <span>{formatPrice(order.valorTotal)}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
