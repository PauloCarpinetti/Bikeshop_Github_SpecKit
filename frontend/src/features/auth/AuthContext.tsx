"use client";

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import * as authService from "@/services/auth";

const ACCESS_TOKEN_KEY = "bikeshop_access_token";
const REFRESH_TOKEN_KEY = "bikeshop_refresh_token";
const CLIENTE_KEY = "bikeshop_cliente";

type Cliente = { clienteId: number; nome: string; email: string; role: string };

type AuthContextValue = {
  cliente: Cliente | null;
  isLoading: boolean;
  register: (nome: string, email: string, senha: string) => Promise<void>;
  login: (email: string, senha: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const stored = window.localStorage.getItem(CLIENTE_KEY);
    if (stored) {
      try {
        setCliente(JSON.parse(stored));
      } catch {
        window.localStorage.removeItem(CLIENTE_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  const persist = useCallback((auth: authService.AuthResponse) => {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
    window.localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
    const clienteData = { clienteId: auth.clienteId, nome: auth.nome, email: auth.email, role: auth.role };
    window.localStorage.setItem(CLIENTE_KEY, JSON.stringify(clienteData));
    setCliente(clienteData);
  }, []);

  const register = useCallback(async (nome: string, email: string, senha: string) => {
    persist(await authService.register(nome, email, senha));
  }, [persist]);

  const login = useCallback(async (email: string, senha: string) => {
    persist(await authService.login(email, senha));
  }, [persist]);

  const logout = useCallback(() => {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
    window.localStorage.removeItem(CLIENTE_KEY);
    setCliente(null);
  }, []);

  return (
    <AuthContext.Provider value={{ cliente, isLoading, register, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  }
  return context;
}
