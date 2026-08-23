import { z } from "zod";
import { apiFetch } from "./apiClient";

export const authResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  clienteId: z.number(),
  nome: z.string(),
  email: z.string(),
});

export type AuthResponse = z.infer<typeof authResponseSchema>;

export async function register(nome: string, email: string, senha: string): Promise<AuthResponse> {
  return apiFetch("/auth/register", authResponseSchema, {
    method: "POST",
    body: { nome, email, senha },
  });
}

export async function login(email: string, senha: string): Promise<AuthResponse> {
  return apiFetch("/auth/login", authResponseSchema, {
    method: "POST",
    body: { email, senha },
  });
}
