"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { searchProducts, type ProductSearchResult } from "@/services/catalog";

function formatPrice(value: number | null): string {
  if (value === null) {
    return "Sob consulta";
  }
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export default function ProductsPage() {
  const [query, setQuery] = useState("");
  const [categoria, setCategoria] = useState("");
  const [modalidade, setModalidade] = useState("");
  const [result, setResult] = useState<ProductSearchResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timeout = setTimeout(() => {
      setIsLoading(true);
      setError(null);
      searchProducts({ q: query, categoria, modalidade, size: 24 })
        .then(setResult)
        .catch(() => setError("Não foi possível carregar o catálogo agora."))
        .finally(() => setIsLoading(false));
    }, 250);
    return () => clearTimeout(timeout);
  }, [query, categoria, modalidade]);

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <h1 className="text-2xl font-semibold">Catálogo</h1>

      <div className="mt-4 flex flex-wrap gap-3">
        <input
          type="search"
          placeholder="Buscar produtos..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className="w-full max-w-xs rounded border px-3 py-2 text-sm"
          aria-label="Buscar produtos"
        />
        <select
          value={categoria}
          onChange={(event) => setCategoria(event.target.value)}
          className="rounded border px-3 py-2 text-sm"
          aria-label="Filtrar por categoria"
        >
          <option value="">Todas as categorias</option>
          <option value="Bicicleta">Bicicleta</option>
          <option value="Acessório">Acessório</option>
        </select>
        <select
          value={modalidade}
          onChange={(event) => setModalidade(event.target.value)}
          className="rounded border px-3 py-2 text-sm"
          aria-label="Filtrar por modalidade"
        >
          <option value="">Todas as modalidades</option>
          <option value="MTB">MTB</option>
          <option value="Speed">Speed</option>
          <option value="Urbana">Urbana</option>
          <option value="Acessório">Acessório</option>
        </select>
      </div>

      {error && <p className="mt-6 text-sm text-red-600">{error}</p>}
      {isLoading && <p className="mt-6 text-sm text-gray-500">Carregando produtos...</p>}

      {!isLoading && result && result.items.length === 0 && (
        <p className="mt-6 text-sm text-gray-500">Nenhum produto encontrado com esses filtros.</p>
      )}

      <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {result?.items.map((product) => (
          <Link
            key={product.id}
            href={`/products/${product.slug}`}
            className="rounded border p-4 transition hover:shadow-md"
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={product.imagemPrincipal ?? "https://placehold.co/400x300?text=BikeShop"}
              alt={product.nome}
              className="h-40 w-full rounded object-cover"
            />
            <p className="mt-3 text-xs uppercase text-gray-500">{product.categoria}</p>
            <h2 className="text-base font-medium">{product.nome}</h2>
            <p className="mt-1 text-sm font-semibold">{formatPrice(product.precoMinimo)}</p>
          </Link>
        ))}
      </div>
    </main>
  );
}
