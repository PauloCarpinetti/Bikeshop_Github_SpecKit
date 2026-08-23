"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCart } from "@/features/cart/CartContext";
import { useAuth } from "@/features/auth/AuthContext";

export function Header() {
  const router = useRouter();
  const { cart, openDrawer } = useCart();
  const { cliente, logout } = useAuth();
  const itemCount = cart?.itens.reduce((total, item) => total + item.quantidade, 0) ?? 0;

  function handleLogout() {
    logout();
    router.push("/products");
  }

  return (
    <header className="border-b bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link href="/products" className="text-xl font-semibold">
          BikeShop
        </Link>

        <div className="flex items-center gap-3">
          {cliente ? (
            <div className="flex items-center gap-2 text-sm">
              <Link href="/profile" className="text-gray-600 hover:underline">Olá, {cliente.nome.split(" ")[0]}</Link>
              <Link href="/orders" className="text-gray-500 hover:underline">Meus pedidos</Link>
              <button onClick={handleLogout} className="text-gray-500 underline hover:text-gray-900">
                Sair
              </button>
            </div>
          ) : (
            <Link href="/login" className="text-sm font-medium hover:underline">
              Entrar
            </Link>
          )}

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
      </div>
    </header>
  );
}
