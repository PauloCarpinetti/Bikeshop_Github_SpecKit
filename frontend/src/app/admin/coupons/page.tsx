"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRequireAdmin } from "@/features/admin/useRequireAdmin";
import { ApiRequestError } from "@/services/apiClient";
import { createCoupon, deactivateCoupon, listCoupons, type Coupon, type CouponInput } from "@/services/admin";

// `datetime-local` não carrega timezone — o valor exibido/editado é sempre a hora local do
// navegador. toISOString() é UTC, então fatiar direto desalinha (mostra/envia horas erradas
// dependendo do fuso). Aqui deslocamos pelo offset local antes de formatar, e desfazemos ao ler.
function toDatetimeLocal(date: Date): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function fromDatetimeLocal(value: string): string {
  return new Date(value).toISOString();
}

const EMPTY_FORM = {
  codigo: "",
  tipo: "PERCENTUAL",
  valor: "10",
  validoDe: toDatetimeLocal(new Date()),
  validoAte: toDatetimeLocal(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)),
  valorMinimoCarrinho: "",
  categoriasAplicaveis: "",
  limiteDeUso: "",
};

export default function AdminCouponsPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [coupons, setCoupons] = useState<Coupon[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [isSaving, setIsSaving] = useState(false);

  const loadCoupons = useCallback(() => {
    listCoupons()
      .then(setCoupons)
      .catch(() => setError("Não foi possível carregar os cupons agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadCoupons();
  }, [isAuthorized, loadCoupons]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-3xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSaving(true);
    setError(null);
    try {
      const input: CouponInput = {
        tipo: form.tipo,
        valor: Number(form.valor),
        validoDe: fromDatetimeLocal(form.validoDe),
        validoAte: fromDatetimeLocal(form.validoAte),
        valorMinimoCarrinho: form.valorMinimoCarrinho ? Number(form.valorMinimoCarrinho) : undefined,
        categoriasAplicaveis: form.categoriasAplicaveis
          ? form.categoriasAplicaveis.split(",").map((c) => c.trim()).filter(Boolean)
          : undefined,
        limiteDeUso: form.limiteDeUso ? Number(form.limiteDeUso) : undefined,
      };
      await createCoupon(form.codigo.toUpperCase(), input);
      setIsFormOpen(false);
      setForm(EMPTY_FORM);
      loadCoupons();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível criar o cupom agora.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDeactivate(id: number) {
    setError(null);
    try {
      await deactivateCoupon(id);
      loadCoupons();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível desativar o cupom agora.");
    }
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Cupons (backoffice)</h1>
        <div className="flex gap-3 text-sm">
          <Link href="/admin/products" className="underline">Produtos</Link>
          <Link href="/admin/orders" className="underline">Pedidos</Link>
          <button type="button" onClick={() => setIsFormOpen((open) => !open)}
                  className="rounded bg-gray-900 px-3 py-1.5 font-medium text-white hover:bg-gray-700">
            {isFormOpen ? "Cancelar" : "Novo cupom"}
          </button>
        </div>
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      {isFormOpen && (
        <form onSubmit={handleSubmit} className="mt-6 grid grid-cols-2 gap-3 rounded border p-4 text-sm">
          <div>
            <label htmlFor="c-codigo" className="block font-medium">Código</label>
            <input id="c-codigo" required value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })}
                   className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-tipo" className="block font-medium">Tipo</label>
            <select id="c-tipo" value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}
                    className="mt-1 w-full rounded border px-2 py-1">
              <option value="PERCENTUAL">Percentual (%)</option>
              <option value="VALOR_FIXO">Valor fixo (R$)</option>
            </select>
          </div>
          <div>
            <label htmlFor="c-valor" className="block font-medium">Valor</label>
            <input id="c-valor" required type="number" step="0.01" value={form.valor}
                   onChange={(e) => setForm({ ...form, valor: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-limite" className="block font-medium">Limite de uso (opcional)</label>
            <input id="c-limite" type="number" value={form.limiteDeUso}
                   onChange={(e) => setForm({ ...form, limiteDeUso: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-validode" className="block font-medium">Válido de</label>
            <input id="c-validode" required type="datetime-local" value={form.validoDe}
                   onChange={(e) => setForm({ ...form, validoDe: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-validoate" className="block font-medium">Válido até</label>
            <input id="c-validoate" required type="datetime-local" value={form.validoAte}
                   onChange={(e) => setForm({ ...form, validoAte: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-minimo" className="block font-medium">Valor mínimo do carrinho (opcional)</label>
            <input id="c-minimo" type="number" step="0.01" value={form.valorMinimoCarrinho}
                   onChange={(e) => setForm({ ...form, valorMinimoCarrinho: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div>
            <label htmlFor="c-categorias" className="block font-medium">Categorias (opcional, separadas por vírgula)</label>
            <input id="c-categorias" value={form.categoriasAplicaveis}
                   onChange={(e) => setForm({ ...form, categoriasAplicaveis: e.target.value })}
                   placeholder="Bicicleta, Acessório" className="mt-1 w-full rounded border px-2 py-1" />
          </div>
          <div className="col-span-2">
            <button type="submit" disabled={isSaving}
                    className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300">
              {isSaving ? "Salvando..." : "Criar cupom"}
            </button>
          </div>
        </form>
      )}

      <ul className="mt-6 space-y-2">
        {coupons === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {coupons?.map((coupon) => {
          const expirado = new Date(coupon.validoAte) < new Date();
          return (
            <li key={coupon.id} className="flex items-center justify-between rounded border p-3 text-sm">
              <div>
                <p className="font-medium">
                  {coupon.codigo} — {coupon.tipo === "PERCENTUAL" ? `${coupon.valor}%` : `R$ ${coupon.valor.toFixed(2)}`}
                  {expirado && <span className="ml-2 text-xs text-red-600">(expirado)</span>}
                </p>
                <p className="text-xs text-gray-500">
                  válido até {new Date(coupon.validoAte).toLocaleString("pt-BR")} · usos: {coupon.usosRealizados}
                  {coupon.limiteDeUso ? `/${coupon.limiteDeUso}` : ""}
                  {coupon.categoriasAplicaveis.length > 0 && ` · categorias: ${coupon.categoriasAplicaveis.join(", ")}`}
                </p>
              </div>
              {!expirado && (
                <button type="button" onClick={() => handleDeactivate(coupon.id)} className="text-red-600 underline">
                  Desativar
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </main>
  );
}
