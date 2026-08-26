"use client";

import { useCallback, useEffect, useState } from "react";
import { useRequireAdmin } from "@/features/admin/guards";
import { AdminNav } from "@/features/admin/AdminNav";
import { ApiRequestError } from "@/services/apiClient";
import { adjustStock, listProducts } from "@/services/admin";
import type { ProductDetail } from "@/services/catalog";

export default function AdminInventoryPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [products, setProducts] = useState<ProductDetail[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adjustments, setAdjustments] = useState<Record<string, string>>({});
  const [motivos, setMotivos] = useState<Record<string, string>>({});
  const [savingSku, setSavingSku] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const loadProducts = useCallback(() => {
    listProducts()
      .then(setProducts)
      .catch(() => setError("Não foi possível carregar os produtos agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadProducts();
  }, [isAuthorized, loadProducts]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-3xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  async function handleAdjust(sku: string) {
    const ajuste = Number(adjustments[sku] ?? "0");
    if (!ajuste) {
      setError("Informe um valor de ajuste diferente de zero.");
      return;
    }
    setSavingSku(sku);
    setError(null);
    setFeedback(null);
    try {
      const updated = await adjustStock(sku, ajuste, motivos[sku]);
      setFeedback(`Estoque de ${sku} atualizado para ${updated.estoqueDisponivel}.`);
      setAdjustments((current) => ({ ...current, [sku]: "" }));
      loadProducts();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível ajustar o estoque agora.");
    } finally {
      setSavingSku(null);
    }
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Ajuste de estoque</h1>
        <AdminNav current="/admin/inventory" />
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {feedback && <p className="mt-4 rounded bg-green-50 p-3 text-sm text-green-700">{feedback}</p>}

      <div className="mt-6 space-y-4">
        {products === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {products?.map((product) => (
          <div key={product.id} className="rounded border p-4">
            <p className="font-medium">{product.nome}</p>
            <ul className="mt-2 space-y-2">
              {product.variacoes.map((variant) => (
                <li key={variant.id} className="flex items-center gap-3 text-sm">
                  <span className="w-56 truncate">{variant.sku}</span>
                  <span className="w-20 text-gray-600">estoque: {variant.estoqueDisponivel}</span>
                  <label htmlFor={`ajuste-${variant.sku}`} className="sr-only">Ajuste de estoque para {variant.sku}</label>
                  <input
                    id={`ajuste-${variant.sku}`}
                    type="number"
                    placeholder="+10 ou -5"
                    value={adjustments[variant.sku] ?? ""}
                    onChange={(e) => setAdjustments((current) => ({ ...current, [variant.sku]: e.target.value }))}
                    className="w-24 rounded border px-2 py-1"
                  />
                  <label htmlFor={`motivo-${variant.sku}`} className="sr-only">Motivo do ajuste para {variant.sku}</label>
                  <input
                    id={`motivo-${variant.sku}`}
                    type="text"
                    placeholder="Motivo (opcional)"
                    value={motivos[variant.sku] ?? ""}
                    onChange={(e) => setMotivos((current) => ({ ...current, [variant.sku]: e.target.value }))}
                    className="flex-1 rounded border px-2 py-1"
                  />
                  <button
                    type="button"
                    onClick={() => handleAdjust(variant.sku)}
                    disabled={savingSku === variant.sku}
                    className="rounded bg-gray-900 px-3 py-1 text-xs font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
                  >
                    {savingSku === variant.sku ? "Salvando..." : "Aplicar"}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </main>
  );
}
