"use client";

import Link from "next/link";
import { useCart } from "@/features/cart/CartContext";

export function Header() {
  const { cart, openDrawer } = useCart();
  const itemCount = cart?.itens.reduce((total, item) => total + item.quantidade, 0) ?? 0;

  return (
    <header className="border-b bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link href="/products" className="text-xl font-semibold">
          BikeShop
        </Link>
        <button
          onClick={openDrawer}
          className="relative rounded border px-3 py-2 text-sm font-medium hover:bg-gray-50"
          aria-label="Abrir carrinho"
        >
          Carrinho
          {itemCount > 0 && (
            <span className="ml-2 rounded-full bg-gray-900 px-2 py-0.5 text-xs text-white">{itemCount}</span>
          )}
        </button>
      </div>
    </header>
  );
}
