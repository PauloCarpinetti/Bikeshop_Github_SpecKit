"use client";

import { useState } from "react";
import { ApiRequestError } from "@/services/apiClient";
import { requestReturn } from "@/services/postsale";
import type { Order } from "@/services/checkout";

type ReturnRequestFormProps = {
  orderId: number;
  onRequested: (order: Order) => void;
};

export function ReturnRequestForm({ orderId, onRequested }: ReturnRequestFormProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      const updated = await requestReturn(orderId, motivo);
      onRequested(updated);
      setIsOpen(false);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível registrar a solicitação agora.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="rounded border px-3 py-1.5 text-sm font-medium hover:bg-gray-50"
      >
        Solicitar troca/devolução
      </button>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-2 rounded border p-3">
      <label htmlFor="return-motivo" className="block text-sm font-medium">
        Motivo da troca/devolução
      </label>
      <textarea
        id="return-motivo"
        required
        value={motivo}
        onChange={(event) => setMotivo(event.target.value)}
        rows={3}
        className="w-full rounded border px-3 py-2 text-sm"
        placeholder="Descreva o problema ou o motivo da troca..."
      />
      {error && <p className="text-xs text-red-600">{error}</p>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
        >
          {isSubmitting ? "Enviando..." : "Enviar solicitação"}
        </button>
        <button
          type="button"
          onClick={() => setIsOpen(false)}
          className="rounded border px-4 py-2 text-sm font-medium hover:bg-gray-50"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
