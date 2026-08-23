import { z } from "zod";
import { apiFetch } from "./apiClient";

export const shippingQuoteSchema = z.object({
  transportadora: z.string(),
  valor: z.number(),
  prazoDias: z.number(),
  estimado: z.boolean(),
});

export const enderecoEntregaSchema = z.object({
  cep: z.string().min(1, "Informe o CEP"),
  logradouro: z.string().min(1, "Informe o logradouro"),
  numero: z.string().min(1, "Informe o número"),
  complemento: z.string().optional(),
  bairro: z.string().min(1, "Informe o bairro"),
  cidade: z.string().min(1, "Informe a cidade"),
  estado: z.string().min(2, "Informe o estado (UF)").max(2, "UF deve ter 2 letras"),
});

export const paymentProviderSchema = z.enum(["STRIPE", "MERCADO_PAGO", "PAGSEGURO"]);
export type PaymentProvider = z.infer<typeof paymentProviderSchema>;
export type EnderecoEntrega = z.infer<typeof enderecoEntregaSchema>;
export type ShippingQuote = z.infer<typeof shippingQuoteSchema>;

export const orderItemSchema = z.object({
  variacaoProdutoId: z.number(),
  sku: z.string(),
  nomeProduto: z.string(),
  precoUnitario: z.number(),
  quantidade: z.number(),
  subtotal: z.number(),
});

export const orderStatusHistoryEntrySchema = z.object({
  status: z.string(),
  timestamp: z.string(),
});

export const orderSchema = z.object({
  id: z.number(),
  status: z.string(),
  valorItens: z.number(),
  valorFrete: z.number(),
  valorTotal: z.number(),
  transportadora: z.string().nullable(),
  prazoFreteDias: z.number().nullable(),
  paymentProvider: z.string().nullable(),
  paymentReference: z.string().nullable(),
  paymentStatus: z.string().nullable(),
  criadoEm: z.string(),
  statusHistorico: z.array(orderStatusHistoryEntrySchema),
  enderecoEntrega: enderecoEntregaSchema.nullable(),
  itens: z.array(orderItemSchema),
});

export const checkoutResultSchema = z.object({
  pedido: orderSchema,
  paymentRedirectUrl: z.string().nullable(),
  pagamentoSimulado: z.boolean(),
});

export type Order = z.infer<typeof orderSchema>;
export type CheckoutResult = z.infer<typeof checkoutResultSchema>;

export async function quoteShipping(cep: string): Promise<ShippingQuote> {
  return apiFetch("/checkout/shipping-quote", shippingQuoteSchema, {
    method: "POST",
    body: { cep },
  });
}

export type CreateOrderInput = {
  clienteNome: string;
  clienteEmail: string;
  endereco: EnderecoEntrega;
  paymentProvider: PaymentProvider;
};

export async function createOrder(input: CreateOrderInput): Promise<CheckoutResult> {
  return apiFetch("/checkout/orders", checkoutResultSchema, {
    method: "POST",
    body: input,
  });
}
