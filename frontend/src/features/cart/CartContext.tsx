"use client";

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { addCartItem, getCart, removeCartItem, updateCartItem, type CartView } from "@/services/cart";

type CartContextValue = {
  cart: CartView | null;
  isLoading: boolean;
  error: string | null;
  isDrawerOpen: boolean;
  openDrawer: () => void;
  closeDrawer: () => void;
  addItem: (variacaoProdutoId: number, quantidade: number) => Promise<void>;
  updateItem: (variacaoProdutoId: number, quantidade: number) => Promise<void>;
  removeItem: (variacaoProdutoId: number) => Promise<void>;
  /** Recarrega o carrinho do backend — usado após o checkout para refletir que ele foi esvaziado. */
  refresh: () => Promise<void>;
};

const CartContext = createContext<CartContextValue | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<CartView | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setCart(await getCart());
    } catch {
      setError("Não foi possível carregar o carrinho.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addItem = useCallback(async (variacaoProdutoId: number, quantidade: number) => {
    setError(null);
    try {
      setCart(await addCartItem(variacaoProdutoId, quantidade));
      setIsDrawerOpen(true);
    } catch {
      setError("Não foi possível adicionar o item ao carrinho (verifique o estoque disponível).");
    }
  }, []);

  const updateItem = useCallback(async (variacaoProdutoId: number, quantidade: number) => {
    setError(null);
    try {
      setCart(await updateCartItem(variacaoProdutoId, quantidade));
    } catch {
      setError("Não foi possível atualizar a quantidade (verifique o estoque disponível).");
    }
  }, []);

  const removeItem = useCallback(async (variacaoProdutoId: number) => {
    setError(null);
    try {
      setCart(await removeCartItem(variacaoProdutoId));
    } catch {
      setError("Não foi possível remover o item do carrinho.");
    }
  }, []);

  return (
    <CartContext.Provider
      value={{
        cart,
        isLoading,
        error,
        isDrawerOpen,
        openDrawer: () => setIsDrawerOpen(true),
        closeDrawer: () => setIsDrawerOpen(false),
        addItem,
        updateItem,
        removeItem,
        refresh,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart deve ser usado dentro de um CartProvider");
  }
  return context;
}
