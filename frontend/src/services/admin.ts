import { z } from "zod";
import { apiFetch } from "./apiClient";
import { productDetailSchema, variantSchema, type ProductDetail, type Variant } from "./catalog";
import { orderSchema, type Order } from "./checkout";

export type VariantInput = {
  sku: string;
  atributos: Record<string, unknown>;
  preco: number;
  estoqueDisponivel: number;
  pesoKg: number;
  alturaCm: number;
  larguraCm: number;
  comprimentoCm: number;
};

export type ProductInput = {
  nome: string;
  descricao?: string;
  categoria: string;
  marca?: string;
  modalidade?: string;
  especificacoesTecnicas: Record<string, unknown>;
  tabelaGeometria: Record<string, unknown>;
  imagens: string[];
};

export type CreateProductInput = ProductInput & { variantes: VariantInput[] };

export type UpdateVariantInput = {
  atributos: Record<string, unknown>;
  preco: number;
  status: string;
  pesoKg: number;
  alturaCm: number;
  larguraCm: number;
  comprimentoCm: number;
};

export async function listProducts(): Promise<ProductDetail[]> {
  return apiFetch("/admin/products", z.array(productDetailSchema));
}

export async function getProduct(id: number): Promise<ProductDetail> {
  return apiFetch(`/admin/products/${id}`, productDetailSchema);
}

export async function createProduct(input: CreateProductInput): Promise<ProductDetail> {
  return apiFetch("/admin/products", productDetailSchema, { method: "POST", body: input });
}

export async function updateProduct(id: number, input: ProductInput): Promise<ProductDetail> {
  return apiFetch(`/admin/products/${id}`, productDetailSchema, { method: "PUT", body: input });
}

export async function deactivateProduct(id: number): Promise<void> {
  // DELETE responde 204 sem corpo — z.unknown() aceita o `null` que o apiFetch produz nesse caso.
  await apiFetch(`/admin/products/${id}`, z.unknown(), { method: "DELETE" });
}

export async function addVariant(productId: number, input: VariantInput): Promise<Variant> {
  return apiFetch(`/admin/products/${productId}/variants`, variantSchema, { method: "POST", body: input });
}

export async function updateVariant(productId: number, variantId: number, input: UpdateVariantInput): Promise<Variant> {
  return apiFetch(`/admin/products/${productId}/variants/${variantId}`, variantSchema, { method: "PUT", body: input });
}

export async function adjustStock(sku: string, ajuste: number, motivo?: string): Promise<Variant> {
  return apiFetch(`/admin/products/${sku}/stock`, variantSchema, { method: "PATCH", body: { ajuste, motivo } });
}

export async function listAdminOrders(): Promise<Order[]> {
  return apiFetch("/admin/orders", z.array(orderSchema));
}

export async function updateOrderStatus(id: number, status: string): Promise<Order> {
  return apiFetch(`/admin/orders/${id}`, orderSchema, { method: "PATCH", body: { status } });
}

export const couponSchema = z.object({
  id: z.number(),
  codigo: z.string(),
  tipo: z.string(),
  valor: z.number(),
  validoDe: z.string(),
  validoAte: z.string(),
  valorMinimoCarrinho: z.number().nullable(),
  categoriasAplicaveis: z.array(z.string()),
  limiteDeUso: z.number().nullable(),
  usosRealizados: z.number(),
});

export type Coupon = z.infer<typeof couponSchema>;

export type CouponInput = {
  tipo: string;
  valor: number;
  validoDe: string;
  validoAte: string;
  valorMinimoCarrinho?: number;
  categoriasAplicaveis?: string[];
  limiteDeUso?: number;
};

export async function listCoupons(): Promise<Coupon[]> {
  return apiFetch("/admin/coupons", z.array(couponSchema));
}

export async function createCoupon(codigo: string, input: CouponInput): Promise<Coupon> {
  return apiFetch("/admin/coupons", couponSchema, { method: "POST", body: { codigo, ...input } });
}

export async function updateCoupon(id: number, input: CouponInput): Promise<Coupon> {
  return apiFetch(`/admin/coupons/${id}`, couponSchema, { method: "PUT", body: input });
}

export async function deactivateCoupon(id: number): Promise<void> {
  await apiFetch(`/admin/coupons/${id}`, z.unknown(), { method: "DELETE" });
}

export const customerSchema = z.object({
  id: z.number(),
  nome: z.string(),
  email: z.string(),
  telefone: z.string().nullable(),
  bloqueado: z.boolean(),
  criadoEm: z.string(),
});

export type Customer = z.infer<typeof customerSchema>;

export async function listCustomers(): Promise<Customer[]> {
  return apiFetch("/admin/customers", z.array(customerSchema));
}

export async function updateCustomerStatus(id: number, bloqueado: boolean): Promise<Customer> {
  return apiFetch(`/admin/customers/${id}/status`, customerSchema, { method: "PATCH", body: { bloqueado } });
}

export const auditLogSchema = z.object({
  id: z.number(),
  actor: z.string(),
  actorRole: z.string(),
  action: z.string(),
  entityName: z.string(),
  entityId: z.string(),
  previousState: z.string().nullable(),
  newState: z.string().nullable(),
  occurredAt: z.string(),
});

export type AuditLogEntry = z.infer<typeof auditLogSchema>;

export async function listAuditLogs(): Promise<AuditLogEntry[]> {
  return apiFetch("/admin/audit-logs", z.array(auditLogSchema));
}

export const reviewAdminSchema = z.object({
  id: z.number(),
  produtoId: z.number(),
  clienteId: z.number(),
  pedidoId: z.number(),
  nota: z.number(),
  comentario: z.string().nullable(),
  status: z.string(),
  criadoEm: z.string(),
});

export type ReviewAdmin = z.infer<typeof reviewAdminSchema>;

export async function listReviewsForModeration(): Promise<ReviewAdmin[]> {
  return apiFetch("/admin/reviews", z.array(reviewAdminSchema));
}

export async function moderateReview(id: number, aprovado: boolean): Promise<ReviewAdmin> {
  return apiFetch(`/admin/reviews/${id}`, reviewAdminSchema, { method: "PATCH", body: { aprovado } });
}
