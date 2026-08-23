import { z } from "zod";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8082/api/v1";

export const apiErrorSchema = z.object({
  code: z.string(),
  message: z.string(),
  details: z.array(z.string()).optional(),
  timestamp: z.string(),
});

export type ApiError = z.infer<typeof apiErrorSchema>;

export class ApiRequestError extends Error {
  constructor(public readonly status: number, public readonly error: ApiError) {
    super(error.message);
  }
}

type RequestOptions = Omit<RequestInit, "body"> & { body?: unknown };

/**
 * Client HTTP base compartilhado por todas as features. Cada feature deve validar
 * a resposta com seu próprio schema Zod antes de usá-la.
 */
export async function apiFetch<T>(path: string, schema: z.ZodType<T>, options: RequestOptions = {}): Promise<T> {
  const token = typeof window !== "undefined" ? window.localStorage.getItem("bikeshop_access_token") : null;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    // Necessário para o cookie de carrinho de visitante (bikeshop_cart_id) trafegar entre as
    // origens do frontend (3002) e do backend (8082) — ver SecurityConfig.corsConfigurationSource.
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  const json = await response.json().catch(() => null);

  if (!response.ok) {
    const parsedError = apiErrorSchema.safeParse(json);
    throw new ApiRequestError(
      response.status,
      parsedError.success
        ? parsedError.data
        : { code: "UNKNOWN_ERROR", message: "Erro desconhecido na comunicação com a API", timestamp: new Date().toISOString() },
    );
  }

  return schema.parse(json);
}
