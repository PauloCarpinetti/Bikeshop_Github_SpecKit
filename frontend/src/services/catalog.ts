import { z } from "zod";
import { apiFetch } from "./apiClient";

export const variantSchema = z.object({
  id: z.number(),
  sku: z.string(),
  atributos: z.record(z.unknown()),
  preco: z.number(),
  estoqueDisponivel: z.number(),
  status: z.string(),
});

export const productSummarySchema = z.object({
  id: z.number(),
  slug: z.string(),
  nome: z.string(),
  categoria: z.string(),
  marca: z.string().nullable(),
  modalidade: z.string().nullable(),
  precoMinimo: z.number().nullable(),
  precoMaximo: z.number().nullable(),
  imagemPrincipal: z.string().nullable(),
});

export const productSearchResultSchema = z.object({
  items: z.array(productSummarySchema),
  total: z.number(),
  page: z.number(),
  size: z.number(),
});

export const productDetailSchema = z.object({
  id: z.number(),
  slug: z.string(),
  nome: z.string(),
  descricao: z.string().nullable(),
  categoria: z.string(),
  marca: z.string().nullable(),
  modalidade: z.string().nullable(),
  especificacoesTecnicas: z.record(z.unknown()),
  tabelaGeometria: z.record(z.unknown()),
  imagens: z.array(z.string()),
  variacoes: z.array(variantSchema),
});

export type ProductSummary = z.infer<typeof productSummarySchema>;
export type ProductSearchResult = z.infer<typeof productSearchResultSchema>;
export type ProductDetail = z.infer<typeof productDetailSchema>;
export type Variant = z.infer<typeof variantSchema>;

export type ProductFilters = {
  q?: string;
  categoria?: string;
  marca?: string;
  modalidade?: string;
  tamanho?: string;
  precoMin?: number;
  precoMax?: number;
  page?: number;
  size?: number;
};

export async function searchProducts(filters: ProductFilters = {}): Promise<ProductSearchResult> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      params.set(key, String(value));
    }
  });
  const query = params.toString();
  return apiFetch(`/catalog/products${query ? `?${query}` : ""}`, productSearchResultSchema);
}

export async function getProductBySlug(slug: string): Promise<ProductDetail> {
  return apiFetch(`/catalog/products/${slug}`, productDetailSchema);
}
