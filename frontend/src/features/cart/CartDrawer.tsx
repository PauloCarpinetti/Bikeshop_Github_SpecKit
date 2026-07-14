"use client";

import { useCart } from "./CartContext";

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export function CartDrawer() {
  const { cart, isLoading, error, isDrawerOpen, closeDrawer, updateItem, removeItem } = useCart();

  if (!isDrawerOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40" role="dialog" aria-modal="true" aria-label="Carrinho de compras">
      <div className="flex h-full w-full max-w-sm flex-col bg-white p-4 shadow-xl">
        <div className="flex items-center justify-between border-b pb-3">
          <h2 className="text-lg font-semibold">Meu carrinho</h2>
          <button onClick={closeDrawer} aria-label="Fechar carrinho" className="rounded p-1 text-gray-500 hover:bg-gray-100">
            ✕
          </button>
        </div>

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

        {isLoading && <p className="mt-4 text-sm text-gray-500">Carregando carrinho...</p>}

        {!isLoading && cart && cart.itens.length === 0 && (
          <p className="mt-4 text-sm text-gray-500">Seu carrinho está vazio.</p>
        )}

        <ul className="mt-4 flex-1 space-y-4 overflow-y-auto">
          {cart?.itens.map((item) => (
            <li key={item.variacaoProdutoId} className="flex gap-3 border-b pb-4">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={item.imagem ?? "https://placehold.co/80x80?text=Bike"}
                alt={item.nomeProduto}
                className="h-16 w-16 rounded object-cover"
              />
              <div className="flex-1">
                <p className="text-sm font-medium">{item.nomeProduto}</p>
                <p className="text-xs text-gray-500">SKU: {item.sku}</p>
                <div className="mt-2 flex items-center gap-2">
                  <label className="text-xs text-gray-500" htmlFor={`qtd-${item.variacaoProdutoId}`}>
                    Qtd.
                  </label>
                  <input
                    id={`qtd-${item.variacaoProdutoId}`}
                    type="number"
                    min={0}
                    value={item.quantidade}
                    onChange={(event) => updateItem(item.variacaoProdutoId, Number(event.target.value))}
                    className="w-16 rounded border px-2 py-1 text-sm"
                  />
                  <button
                    onClick={() => removeItem(item.variacaoProdutoId)}
                    className="ml-auto text-xs text-red-600 hover:underline"
                  >
                    Remover
                  </button>
                </div>
              </div>
              <p className="text-sm font-semibold">{formatPrice(item.subtotal)}</p>
            </li>
          ))}
        </ul>

        {cart && cart.itens.length > 0 && (
          <div className="border-t pt-4">
            <div className="flex justify-between text-base font-semibold">
              <span>Total</span>
              <span>{formatPrice(cart.total)}</span>
            </div>
            <p className="mt-2 text-xs text-gray-500">
              Frete e pagamento serão calculados no checkout (próxima etapa da implementação).
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
