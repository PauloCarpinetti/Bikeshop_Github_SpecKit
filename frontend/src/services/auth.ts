import { z } from "zod";
import { apiFetch } from "./apiClient";

export const authResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  clienteId: z.number(),
  nome: z.string(),
  email: z.string(),
  role: z.string(),
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

/**
 * Revoga o access token (lido do header Authorization pelo apiFetch) e, se informado, o refresh
 * token — via blacklist no Redis (T093). Sem isso, "Sair" só limpava o localStorage no client; os
 * tokens em si continuavam válidos até expirar mesmo após o logout.
 */
export async function logout(refreshToken: string | null): Promise<void> {
  await apiFetch("/auth/logout", z.unknown(), {
    method: "POST",
    body: { refreshToken },
  });
}
