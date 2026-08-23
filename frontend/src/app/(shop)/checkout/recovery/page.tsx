"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

/**
 * Tela de recuperação de checkout (SC-002): quando a criação do pedido falha (estoque
 * insuficiente, CEP inválido, erro de comunicação com o gateway, etc.), o usuário chega aqui em
 * vez de ver um erro genérico — pode tentar de novo (o carrinho não é alterado até o pedido ser
 * criado com sucesso) ou buscar suporte.
 */
export default function CheckoutRecoveryPage() {
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const stored = window.sessionStorage.getItem("bikeshop_checkout_error");
    setMessage(stored);
    window.sessionStorage.removeItem("bikeshop_checkout_error");
  }, []);

  return (
    <main className="mx-auto max-w-xl px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold">Não conseguimos concluir seu pedido</h1>
      <p className="mt-3 text-gray-600">
        {message ?? "Ocorreu um problema inesperado ao processar seu pedido."}
      </p>
      <p className="mt-2 text-sm text-gray-500">
        Seu carrinho continua salvo — nenhum valor foi cobrado. Você pode tentar novamente.
      </p>

      <div className="mt-8 flex flex-col items-center gap-3">
        <Link
          href="/checkout"
          className="rounded bg-gray-900 px-6 py-3 text-sm font-medium text-white hover:bg-gray-700"
        >
          Tentar novamente
        </Link>
        <Link href="/products" className="text-sm text-gray-500 underline">
          Voltar ao catálogo
        </Link>
      </div>

      <div className="mt-10 rounded border bg-gray-50 p-4 text-sm text-gray-600">
        <p className="font-medium">Precisa de ajuda?</p>
        <p className="mt-1">Fale com o nosso suporte: suporte@bikeshop.example · (11) 4000-0000</p>
      </div>
    </main>
  );
}
