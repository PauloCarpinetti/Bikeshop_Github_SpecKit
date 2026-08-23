"use client";

import { useState } from "react";
import { ApiRequestError } from "@/services/apiClient";
import { createReview } from "@/services/postsale";

type ReviewFormProps = {
  pedidoId: number;
  variacaoProdutoId: number;
  nomeProduto: string;
};

export function ReviewForm({ pedidoId, variacaoProdutoId, nomeProduto }: ReviewFormProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [nota, setNota] = useState(5);
  const [comentario, setComentario] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      await createReview({ pedidoId, variacaoProdutoId, nota, comentario: comentario || undefined });
      setDone(true);
      setIsOpen(false);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível publicar a avaliação agora.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (done) {
    return <p className="text-xs text-green-700">Avaliação publicada para {nomeProduto}.</p>;
  }

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="text-xs underline hover:text-gray-900"
      >
        Avaliar {nomeProduto}
      </button>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-2 rounded border p-3">
      <div>
        <label htmlFor={`nota-${variacaoProdutoId}`} className="block text-xs font-medium">Nota (1 a 5)</label>
        <select
          id={`nota-${variacaoProdutoId}`}
          value={nota}
          onChange={(event) => setNota(Number(event.target.value))}
          className="mt-1 rounded border px-2 py-1 text-sm"
        >
          {[5, 4, 3, 2, 1].map((valor) => (
            <option key={valor} value={valor}>{valor}</option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor={`comentario-${variacaoProdutoId}`} className="block text-xs font-medium">Comentário (opcional)</label>
        <textarea
          id={`comentario-${variacaoProdutoId}`}
          value={comentario}
          onChange={(event) => setComentario(event.target.value)}
          rows={2}
          className="mt-1 w-full rounded border px-2 py-1 text-sm"
        />
      </div>
      {error && <p className="text-xs text-red-600">{error}</p>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-gray-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
        >
          {isSubmitting ? "Enviando..." : "Publicar avaliação"}
        </button>
        <button
          type="button"
          onClick={() => setIsOpen(false)}
          className="rounded border px-3 py-1.5 text-xs font-medium hover:bg-gray-50"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
