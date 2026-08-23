import { z } from "zod";
import { apiFetch } from "./apiClient";
import { orderSchema, type Order } from "./checkout";

export const profileSchema = z.object({
  id: z.number(),
  nome: z.string(),
  email: z.string(),
  telefone: z.string().nullable(),
});

export type Profile = z.infer<typeof profileSchema>;

export const enderecoSchema = z.object({
  id: z.number(),
  cep: z.string(),
  logradouro: z.string(),
  numero: z.string(),
  complemento: z.string().nullable(),
  bairro: z.string(),
  cidade: z.string(),
  estado: z.string(),
  tipo: z.string(),
  padrao: z.boolean(),
});

export type Endereco = z.infer<typeof enderecoSchema>;

export type UpdateProfileInput = {
  nome: string;
  telefone?: string;
  novaSenha?: string;
};

export type EnderecoInput = {
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  estado: string;
  tipo?: string;
  padrao: boolean;
};

export async function getProfile(): Promise<Profile> {
  return apiFetch("/account/profile", profileSchema);
}

export async function updateProfile(input: UpdateProfileInput): Promise<Profile> {
  return apiFetch("/account/profile", profileSchema, { method: "PUT", body: input });
}

export async function listAddresses(): Promise<Endereco[]> {
  return apiFetch("/account/addresses", z.array(enderecoSchema));
}

export async function createAddress(input: EnderecoInput): Promise<Endereco> {
  return apiFetch("/account/addresses", enderecoSchema, { method: "POST", body: input });
}

export async function updateAddress(id: number, input: EnderecoInput): Promise<Endereco> {
  return apiFetch(`/account/addresses/${id}`, enderecoSchema, { method: "PUT", body: input });
}

export async function listOrders(): Promise<Order[]> {
  return apiFetch("/account/orders", z.array(orderSchema));
}

export async function getOrder(id: number): Promise<Order> {
  return apiFetch(`/account/orders/${id}`, orderSchema);
}
