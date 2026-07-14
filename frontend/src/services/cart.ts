import { z } from "zod";
import { apiFetch } from "./apiClient";

export const cartItemViewSchema = z.object({
  variacaoProdutoId: z.number(),
  sku: z.string(),
  nomeProduto: z.string(),
  slugProduto: z.string(),
  imagem: z.string().nullable(),
  precoUnitario: z.number(),
  quantidade: z.number(),
  subtotal: z.number(),
});

export const cartViewSchema = z.object({
  cartId: z.string(),
  itens: z.array(cartItemViewSchema),
  total: z.number(),
});

export type CartItemView = z.infer<typeof cartItemViewSchema>;
export type CartView = z.infer<typeof cartViewSchema>;

export async function getCart(): Promise<CartView> {
  return apiFetch("/cart", cartViewSchema);
}

export async function addCartItem(variacaoProdutoId: number, quantidade: number): Promise<CartView> {
  return apiFetch("/cart/items", cartViewSchema, {
    method: "POST",
    body: { variacaoProdutoId, quantidade },
  });
}

export async function updateCartItem(variacaoProdutoId: number, quantidade: number): Promise<CartView> {
  return apiFetch(`/cart/items/${variacaoProdutoId}`, cartViewSchema, {
    method: "PATCH",
    body: { quantidade },
  });
}

export async function removeCartItem(variacaoProdutoId: number): Promise<CartView> {
  return apiFetch(`/cart/items/${variacaoProdutoId}`, cartViewSchema, {
    method: "DELETE",
  });
}
