"use client";

import { useState } from "react";
import { useCart } from "./CartContext";

export function AddToCartButton({ variacaoProdutoId, disabled }: { variacaoProdutoId: number; disabled?: boolean }) {
  const { addItem } = useCart();
  const [isAdding, setIsAdding] = useState(false);

  async function handleClick() {
    setIsAdding(true);
    try {
      await addItem(variacaoProdutoId, 1);
    } finally {
      setIsAdding(false);
    }
  }

  return (
    <button
      onClick={handleClick}
      disabled={disabled || isAdding}
      className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:cursor-not-allowed disabled:bg-gray-300"
    >
      {isAdding ? "Adicionando..." : "Adicionar ao carrinho"}
    </button>
  );
}
