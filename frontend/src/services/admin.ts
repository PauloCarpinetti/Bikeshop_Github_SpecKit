import { z } from "zod";
import { apiFetch } from "./apiClient";
import { productDetailSchema, variantSchema, type ProductDetail, type Variant } from "./catalog";

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
