"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCart } from "@/features/cart/CartContext";
import { useAuth } from "@/features/auth/AuthContext";
import { ApiRequestError } from "@/services/apiClient";
import {
  createOrder,
  quoteShipping,
  type CheckoutResult,
  type PaymentProvider,
  type ShippingQuote,
} from "@/services/checkout";

const checkoutFormSchema = z.object({
  clienteNome: z.string().min(1, "Informe seu nome"),
  clienteEmail: z.string().email("E-mail inválido"),
  cep: z.string().min(8, "CEP inválido"),
  logradouro: z.string().min(1, "Informe o logradouro"),
  numero: z.string().min(1, "Informe o número"),
  complemento: z.string().optional(),
  bairro: z.string().min(1, "Informe o bairro"),
  cidade: z.string().min(1, "Informe a cidade"),
  estado: z.string().min(2, "UF").max(2, "UF"),
  paymentProvider: z.enum(["STRIPE", "MERCADO_PAGO", "PAGSEGURO"]),
});

type CheckoutFormValues = z.infer<typeof checkoutFormSchema>;

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

const PAYMENT_OPTIONS: { value: PaymentProvider; label: string }[] = [
  { value: "STRIPE", label: "Cartão de crédito (Stripe)" },
  { value: "MERCADO_PAGO", label: "PIX (Mercado Pago)" },
  { value: "PAGSEGURO", label: "Boleto (PagSeguro)" },
];

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, isLoading: isCartLoading, refresh: refreshCart } = useCart();
  const { cliente } = useAuth();
  const [shipping, setShipping] = useState<ShippingQuote | null>(null);
  const [isQuoting, setIsQuoting] = useState(false);
  const [shippingError, setShippingError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<CheckoutResult | null>(null);

  const {
    register,
    handleSubmit,
    getValues,
    setValue,
    formState: { errors },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutFormSchema),
    defaultValues: {
      paymentProvider: "STRIPE",
      clienteNome: cliente?.nome ?? "",
      clienteEmail: cliente?.email ?? "",
    },
  });

  useEffect(() => {
    if (cliente) {
      setValue("clienteNome", cliente.nome);
      setValue("clienteEmail", cliente.email);
    }
  }, [cliente, setValue]);

  async function handleQuoteShipping() {
    const cep = getValues("cep");
    if (!cep || cep.length < 8) {
      setShippingError("Informe um CEP válido antes de calcular o frete.");
      return;
    }
    setIsQuoting(true);
    setShippingError(null);
    try {
      setShipping(await quoteShipping(cep));
    } catch (error) {
      setShippingError(
        error instanceof ApiRequestError ? error.error.message : "Não foi possível calcular o frete agora.",
      );
    } finally {
      setIsQuoting(false);
    }
  }

  async function onSubmit(values: CheckoutFormValues) {
    setIsSubmitting(true);
    try {
      const checkoutResult = await createOrder({
        clienteNome: values.clienteNome,
        clienteEmail: values.clienteEmail,
        paymentProvider: values.paymentProvider,
        endereco: {
          cep: values.cep,
          logradouro: values.logradouro,
          numero: values.numero,
          complemento: values.complemento,
          bairro: values.bairro,
          cidade: values.cidade,
          estado: values.estado,
        },
      });
      setResult(checkoutResult);
      await refreshCart();
    } catch (error) {
      const message = error instanceof ApiRequestError
        ? error.error.message
        : "Não foi possível concluir o pedido. Tente novamente.";
      if (typeof window !== "undefined") {
        window.sessionStorage.setItem("bikeshop_checkout_error", message);
      }
      router.push("/checkout/recovery");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (result) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center">
        <h1 className="text-2xl font-semibold">Pedido criado com sucesso!</h1>
        <p className="mt-2 text-gray-600">Pedido #{result.pedido.id} — status: {result.pedido.status}</p>
        {result.pagamentoSimulado && (
          <p className="mt-2 rounded bg-amber-50 p-3 text-sm text-amber-800">
            Pagamento em modo simulado (sem credenciais reais de gateway configuradas neste ambiente).
          </p>
        )}
        <dl className="mx-auto mt-6 max-w-xs space-y-1 text-left text-sm">
          <div className="flex justify-between"><dt>Itens</dt><dd>{formatPrice(result.pedido.valorItens)}</dd></div>
          <div className="flex justify-between"><dt>Frete ({result.pedido.transportadora})</dt><dd>{formatPrice(result.pedido.valorFrete)}</dd></div>
          <div className="flex justify-between font-semibold"><dt>Total</dt><dd>{formatPrice(result.pedido.valorTotal)}</dd></div>
        </dl>
      </main>
    );
  }

  if (isCartLoading) {
    return <p className="mx-auto max-w-2xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  if (!cart || cart.itens.length === 0) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center">
        <p className="text-gray-600">Seu carrinho está vazio.</p>
      </main>
    );
  }

  return (
    <main className="mx-auto grid max-w-4xl grid-cols-1 gap-8 px-6 py-8 md:grid-cols-3">
      <form onSubmit={handleSubmit(onSubmit)} className="md:col-span-2 space-y-4">
        <h1 className="text-2xl font-semibold">Finalizar compra</h1>

        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <label htmlFor="clienteNome" className="block text-sm font-medium">Nome completo</label>
            <input id="clienteNome" {...register("clienteNome")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.clienteNome && <p className="mt-1 text-xs text-red-600">{errors.clienteNome.message}</p>}
          </div>
          <div className="col-span-2">
            <label htmlFor="clienteEmail" className="block text-sm font-medium">E-mail</label>
            <input id="clienteEmail" {...register("clienteEmail")} type="email" className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.clienteEmail && <p className="mt-1 text-xs text-red-600">{errors.clienteEmail.message}</p>}
          </div>

          <div>
            <label htmlFor="cep" className="block text-sm font-medium">CEP</label>
            <div className="mt-1 flex gap-2">
              <input id="cep" {...register("cep")} className="w-full rounded border px-3 py-2 text-sm" placeholder="00000-000" />
              <button type="button" onClick={handleQuoteShipping} disabled={isQuoting}
                      className="whitespace-nowrap rounded border px-3 py-2 text-sm hover:bg-gray-50">
                {isQuoting ? "Calculando..." : "Calcular frete"}
              </button>
            </div>
            {errors.cep && <p className="mt-1 text-xs text-red-600">{errors.cep.message}</p>}
            {shippingError && <p className="mt-1 text-xs text-red-600">{shippingError}</p>}
          </div>
          <div>
            <label htmlFor="estado" className="block text-sm font-medium">Estado (UF)</label>
            <input id="estado" {...register("estado")} maxLength={2} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.estado && <p className="mt-1 text-xs text-red-600">{errors.estado.message}</p>}
          </div>

          <div className="col-span-2">
            <label htmlFor="logradouro" className="block text-sm font-medium">Logradouro</label>
            <input id="logradouro" {...register("logradouro")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.logradouro && <p className="mt-1 text-xs text-red-600">{errors.logradouro.message}</p>}
          </div>
          <div>
            <label htmlFor="numero" className="block text-sm font-medium">Número</label>
            <input id="numero" {...register("numero")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.numero && <p className="mt-1 text-xs text-red-600">{errors.numero.message}</p>}
          </div>
          <div>
            <label htmlFor="complemento" className="block text-sm font-medium">Complemento</label>
            <input id="complemento" {...register("complemento")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
          </div>
          <div className="col-span-2">
            <label htmlFor="bairro" className="block text-sm font-medium">Bairro</label>
            <input id="bairro" {...register("bairro")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.bairro && <p className="mt-1 text-xs text-red-600">{errors.bairro.message}</p>}
          </div>
          <div className="col-span-2">
            <label htmlFor="cidade" className="block text-sm font-medium">Cidade</label>
            <input id="cidade" {...register("cidade")} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            {errors.cidade && <p className="mt-1 text-xs text-red-600">{errors.cidade.message}</p>}
          </div>
        </div>

        <fieldset>
          <legend className="text-sm font-medium">Forma de pagamento</legend>
          <div className="mt-2 space-y-2">
            {PAYMENT_OPTIONS.map((option) => (
              <label key={option.value} className="flex items-center gap-2 text-sm">
                <input type="radio" value={option.value} {...register("paymentProvider")} />
                {option.label}
              </label>
            ))}
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded bg-gray-900 px-4 py-3 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
        >
          {isSubmitting ? "Processando..." : "Finalizar pedido"}
        </button>
      </form>

      <aside className="space-y-4 rounded border p-4">
        <h2 className="text-sm font-semibold">Resumo do pedido</h2>
        <ul className="space-y-2 text-sm">
          {cart.itens.map((item) => (
            <li key={item.variacaoProdutoId} className="flex justify-between">
              <span>{item.nomeProduto} × {item.quantidade}</span>
              <span>{formatPrice(item.subtotal)}</span>
            </li>
          ))}
        </ul>
        <div className="border-t pt-2 text-sm">
          <div className="flex justify-between"><span>Itens</span><span>{formatPrice(cart.total)}</span></div>
          <div className="flex justify-between">
            <span>Frete</span>
            <span>{shipping ? formatPrice(shipping.valor) : "—"}</span>
          </div>
          {shipping && (
            <p className="mt-1 text-xs text-gray-500">
              {shipping.transportadora} · {shipping.prazoDias} dia(s) úteis
              {shipping.estimado && " (estimativa)"}
            </p>
          )}
          <div className="mt-2 flex justify-between text-base font-semibold">
            <span>Total</span>
            <span>{formatPrice(cart.total + (shipping?.valor ?? 0))}</span>
          </div>
        </div>
      </aside>
    </main>
  );
}
