"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { getProductBySlug, type ProductDetail, type Variant } from "@/services/catalog";
import { AddToCartButton } from "@/features/cart/AddToCartButton";

function formatPrice(value: number): string {
  return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export default function ProductDetailPage() {
  const params = useParams<{ slug: string }>();
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [selectedVariant, setSelectedVariant] = useState<Variant | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    setNotFound(false);
    getProductBySlug(params.slug)
      .then((data) => {
        setProduct(data);
        setSelectedVariant(data.variacoes[0] ?? null);
      })
      .catch(() => setNotFound(true))
      .finally(() => setIsLoading(false));
  }, [params.slug]);

  if (isLoading) {
    return <p className="mx-auto max-w-4xl px-6 py-8 text-sm text-gray-500">Carregando produto...</p>;
  }

  if (notFound || !product) {
    return <p className="mx-auto max-w-4xl px-6 py-8 text-sm text-red-600">Produto não encontrado.</p>;
  }

  return (
    <main className="mx-auto grid max-w-4xl grid-cols-1 gap-8 px-6 py-8 md:grid-cols-2">
      <div>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={product.imagens[0] ?? "https://placehold.co/600x400?text=BikeShop"}
          alt={product.nome}
          className="w-full rounded-lg object-cover"
        />
      </div>

      <div>
        <p className="text-xs uppercase text-gray-500">{product.categoria}</p>
        <h1 className="text-2xl font-semibold">{product.nome}</h1>
        {product.descricao && <p className="mt-2 text-gray-600">{product.descricao}</p>}

        {selectedVariant && (
          <p className="mt-4 text-xl font-bold">{formatPrice(selectedVariant.preco)}</p>
        )}

        {product.variacoes.length > 1 && (
          <div className="mt-4">
            <p className="mb-2 text-sm font-medium">Variações</p>
            <div className="flex flex-wrap gap-2">
              {product.variacoes.map((variant) => (
                <button
                  key={variant.id}
                  onClick={() => setSelectedVariant(variant)}
                  className={`rounded border px-3 py-1.5 text-sm ${
                    selectedVariant?.id === variant.id ? "border-gray-900 bg-gray-900 text-white" : "border-gray-300"
                  }`}
                >
                  {String(variant.atributos.tamanho ?? variant.sku)}
                  {variant.atributos.cor ? ` · ${variant.atributos.cor}` : ""}
                </button>
              ))}
            </div>
          </div>
        )}

        {selectedVariant && (
          <p className="mt-2 text-sm text-gray-500">
            {selectedVariant.estoqueDisponivel > 0
              ? `${selectedVariant.estoqueDisponivel} em estoque`
              : "Fora de estoque"}
          </p>
        )}

        <div className="mt-6">
          {selectedVariant && (
            <AddToCartButton variacaoProdutoId={selectedVariant.id} disabled={selectedVariant.estoqueDisponivel <= 0} />
          )}
        </div>

        {Object.keys(product.especificacoesTecnicas).length > 0 && (
          <div className="mt-8">
            <h2 className="text-sm font-semibold">Especificações técnicas</h2>
            <dl className="mt-2 grid grid-cols-2 gap-2 text-sm text-gray-600">
              {Object.entries(product.especificacoesTecnicas).map(([key, value]) => (
                <div key={key}>
                  <dt className="capitalize text-gray-500">{key}</dt>
                  <dd>{String(value)}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}

        {Object.keys(product.tabelaGeometria).length > 0 && (
          <div className="mt-6">
            <h2 className="text-sm font-semibold">Tabela de geometria</h2>
            <dl className="mt-2 grid grid-cols-2 gap-2 text-sm text-gray-600">
              {Object.entries(product.tabelaGeometria).map(([key, value]) => (
                <div key={key}>
                  <dt className="capitalize text-gray-500">{key}</dt>
                  <dd>{String(value)}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}
      </div>
    </main>
  );
}
