import { z } from "zod";
import { apiFetch } from "./apiClient";
import { orderSchema, type Order } from "./checkout";

export const reviewSchema = z.object({
  id: z.number(),
  produtoId: z.number(),
  pedidoId: z.number(),
  nota: z.number(),
  comentario: z.string().nullable(),
  status: z.string(),
  criadoEm: z.string(),
});

export type Review = z.infer<typeof reviewSchema>;

export async function requestReturn(orderId: number, motivo: string): Promise<Order> {
  return apiFetch(`/account/orders/${orderId}/return`, orderSchema, {
    method: "POST",
    body: { motivo },
  });
}

export type CreateReviewInput = {
  pedidoId: number;
  variacaoProdutoId: number;
  nota: number;
  comentario?: string;
};

export async function createReview(input: CreateReviewInput): Promise<Review> {
  return apiFetch("/account/reviews", reviewSchema, {
    method: "POST",
    body: input,
  });
}
