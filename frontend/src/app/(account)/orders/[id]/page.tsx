"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";
import { ApiRequestError } from "@/services/apiClient";
import { getOrder } from "@/services/account";
import type { Order } from "@/services/checkout";

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("pt-BR");
}

const STATUS_LABELS: Record<string, string> = {
  CRIADO: "Criado",
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento",
  PAGO: "Pago",
  EM_SEPARACAO: "Em separação",
  ENVIADO: "Enviado",
  ENTREGUE: "Entregue",
  PAGAMENTO_RECUSADO: "Pagamento recusado",
  CANCELADO: "Cancelado",
  EM_TROCA_DEVOLUCAO: "Em troca/devolução",
};

export default function OrderDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { cliente, isLoading: isAuthLoading } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthLoading && !cliente) {
      router.push("/login");
    }
  }, [isAuthLoading, cliente, router]);

  useEffect(() => {
    if (!cliente) return;
    getOrder(Number(params.id))
      .then(setOrder)
      .catch((err) => setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível carregar o pedido agora."));
  }, [cliente, params.id]);

  if (isAuthLoading || !cliente) {
    return <p className="mx-auto max-w-2xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  if (error) {
    return <p className="mx-auto max-w-2xl px-6 py-16 text-sm text-red-700">{error}</p>;
  }

  if (!order) {
    return <p className="mx-auto max-w-2xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <p className="text-sm text-gray-600"><Link href="/orders" className="underline">Voltar para meus pedidos</Link></p>
      <h1 className="mt-2 text-2xl font-semibold">Pedido #{order.id}</h1>

      <section className="mt-6">
        <h2 className="text-sm font-semibold text-gray-700">Rastreamento</h2>
        <ol className="mt-2 space-y-2 border-l pl-4">
          {order.statusHistorico.map((entry, index) => (
            <li key={index} className="text-sm">
              <p className="font-medium">{STATUS_LABELS[entry.status] ?? entry.status}</p>
              <p className="text-xs text-gray-500">{formatDateTime(entry.timestamp)}</p>
            </li>
          ))}
        </ol>
      </section>

      {order.enderecoEntrega && (
        <section className="mt-6">
          <h2 className="text-sm font-semibold text-gray-700">Endereço de entrega</h2>
          <p className="mt-1 text-sm text-gray-600">
            {order.enderecoEntrega.logradouro}, {order.enderecoEntrega.numero}
            {order.enderecoEntrega.complemento ? ` — ${order.enderecoEntrega.complemento}` : ""}
            <br />
            {order.enderecoEntrega.bairro}, {order.enderecoEntrega.cidade} - {order.enderecoEntrega.estado} · {order.enderecoEntrega.cep}
          </p>
        </section>
      )}

      <section className="mt-6">
        <h2 className="text-sm font-semibold text-gray-700">Itens</h2>
        <ul className="mt-2 space-y-2 text-sm">
          {order.itens.map((item) => (
            <li key={item.variacaoProdutoId} className="flex justify-between">
              <span>{item.nomeProduto} × {item.quantidade}</span>
              <span>{formatPrice(item.subtotal)}</span>
            </li>
          ))}
        </ul>
        <div className="mt-3 space-y-1 border-t pt-3 text-sm">
          <div className="flex justify-between"><span>Itens</span><span>{formatPrice(order.valorItens)}</span></div>
          <div className="flex justify-between"><span>Frete ({order.transportadora})</span><span>{formatPrice(order.valorFrete)}</span></div>
          <div className="flex justify-between font-semibold"><span>Total</span><span>{formatPrice(order.valorTotal)}</span></div>
        </div>
      </section>
    </main>
  );
}
