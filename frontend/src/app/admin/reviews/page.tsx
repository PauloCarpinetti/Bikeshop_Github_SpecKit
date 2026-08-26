"use client";

import { useCallback, useEffect, useState } from "react";
import { useRequireAdmin } from "@/features/admin/guards";
import { AdminNav } from "@/features/admin/AdminNav";
import { ApiRequestError } from "@/services/apiClient";
import { listReviewsForModeration, moderateReview, type ReviewAdmin } from "@/services/admin";

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("pt-BR");
}

export default function AdminReviewsPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [reviews, setReviews] = useState<ReviewAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);

  const loadReviews = useCallback(() => {
    listReviewsForModeration()
      .then(setReviews)
      .catch(() => setError("Não foi possível carregar as avaliações agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadReviews();
  }, [isAuthorized, loadReviews]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-4xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  async function handleModerate(review: ReviewAdmin, aprovado: boolean) {
    setSavingId(review.id);
    setError(null);
    try {
      await moderateReview(review.id, aprovado);
      loadReviews();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível moderar a avaliação agora.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Avaliações (backoffice)</h1>
        <AdminNav current="/admin/reviews" />
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <ul className="mt-6 space-y-2">
        {reviews === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {reviews?.length === 0 && <p className="text-sm text-gray-500">Nenhuma avaliação publicada ainda.</p>}
        {reviews?.map((review) => (
          <li key={review.id} className="rounded border p-3 text-sm">
            <div className="flex items-center justify-between">
              <p className="font-medium">
                Nota {review.nota}/5 — produto #{review.produtoId} · pedido #{review.pedidoId}
                <span className={`ml-2 text-xs ${review.status === "PUBLICADA" ? "text-green-600" : "text-red-600"}`}>
                  ({review.status})
                </span>
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => handleModerate(review, true)}
                  disabled={savingId === review.id || review.status === "PUBLICADA"}
                  className="rounded bg-gray-900 px-3 py-1 text-xs font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
                >
                  Aprovar
                </button>
                <button
                  type="button"
                  onClick={() => handleModerate(review, false)}
                  disabled={savingId === review.id || review.status === "MODERADA"}
                  className="rounded border border-red-600 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 disabled:border-gray-300 disabled:text-gray-300"
                >
                  Rejeitar
                </button>
              </div>
            </div>
            {review.comentario && <p className="mt-1 text-gray-600">&ldquo;{review.comentario}&rdquo;</p>}
            <p className="mt-1 text-xs text-gray-500">
              cliente #{review.clienteId} · publicada em {formatDate(review.criadoEm)}
            </p>
          </li>
        ))}
      </ul>
    </main>
  );
}
