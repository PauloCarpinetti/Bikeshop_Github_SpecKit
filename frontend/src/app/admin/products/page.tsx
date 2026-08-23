"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRequireAdmin } from "@/features/admin/useRequireAdmin";
import { ApiRequestError } from "@/services/apiClient";
import {
  addVariant,
  createProduct,
  deactivateProduct,
  listProducts,
  updateProduct,
  updateVariant,
  type CreateProductInput,
  type ProductInput,
  type UpdateVariantInput,
  type VariantInput,
} from "@/services/admin";
import type { ProductDetail, Variant } from "@/services/catalog";

const EMPTY_PRODUCT_FORM: ProductInput = {
  nome: "",
  descricao: "",
  categoria: "Bicicleta",
  marca: "",
  modalidade: "",
  especificacoesTecnicas: {},
  tabelaGeometria: {},
  imagens: [],
};

type VariantFormState = {
  sku: string;
  tamanho: string;
  cor: string;
  preco: string;
  estoqueDisponivel: string;
  pesoKg: string;
  alturaCm: string;
  larguraCm: string;
  comprimentoCm: string;
  status: string;
};

const EMPTY_VARIANT_FORM: VariantFormState = {
  sku: "",
  tamanho: "",
  cor: "",
  preco: "",
  estoqueDisponivel: "0",
  pesoKg: "1.000",
  alturaCm: "15.00",
  larguraCm: "30.00",
  comprimentoCm: "90.00",
  status: "DISPONIVEL",
};

function toVariantInput(form: VariantFormState): VariantInput {
  return {
    sku: form.sku,
    atributos: { tamanho: form.tamanho, cor: form.cor },
    preco: Number(form.preco),
    estoqueDisponivel: Number(form.estoqueDisponivel),
    pesoKg: Number(form.pesoKg),
    alturaCm: Number(form.alturaCm),
    larguraCm: Number(form.larguraCm),
    comprimentoCm: Number(form.comprimentoCm),
  };
}

function toUpdateVariantInput(form: VariantFormState): UpdateVariantInput {
  return {
    atributos: { tamanho: form.tamanho, cor: form.cor },
    preco: Number(form.preco),
    status: form.status,
    pesoKg: Number(form.pesoKg),
    alturaCm: Number(form.alturaCm),
    larguraCm: Number(form.larguraCm),
    comprimentoCm: Number(form.comprimentoCm),
  };
}

export default function AdminProductsPage() {
  const { isAuthorized, isCheckingAuth } = useRequireAdmin();
  const [products, setProducts] = useState<ProductDetail[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const [productForm, setProductForm] = useState<ProductInput>(EMPTY_PRODUCT_FORM);
  const [variantForms, setVariantForms] = useState<VariantFormState[]>([{ ...EMPTY_VARIANT_FORM }]);
  const [isSaving, setIsSaving] = useState(false);

  const loadProducts = useCallback(() => {
    listProducts()
      .then(setProducts)
      .catch(() => setError("Não foi possível carregar os produtos agora."));
  }, []);

  useEffect(() => {
    if (isAuthorized) loadProducts();
  }, [isAuthorized, loadProducts]);

  if (isCheckingAuth || !isAuthorized) {
    return <p className="mx-auto max-w-4xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  function updateVariantForm(index: number, patch: Partial<VariantFormState>) {
    setVariantForms((current) => current.map((form, i) => (i === index ? { ...form, ...patch } : form)));
  }

  async function handleCreateSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSaving(true);
    setError(null);
    try {
      const input: CreateProductInput = { ...productForm, variantes: variantForms.map(toVariantInput) };
      await createProduct(input);
      setIsCreateOpen(false);
      setProductForm(EMPTY_PRODUCT_FORM);
      setVariantForms([{ ...EMPTY_VARIANT_FORM }]);
      loadProducts();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível criar o produto agora.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDeactivate(id: number) {
    setError(null);
    try {
      await deactivateProduct(id);
      loadProducts();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível inativar o produto agora.");
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Produtos (backoffice)</h1>
        <div className="flex gap-3 text-sm">
          <Link href="/admin/inventory" className="underline">Ajustar estoque</Link>
          <button
            type="button"
            onClick={() => setIsCreateOpen((open) => !open)}
            className="rounded bg-gray-900 px-3 py-1.5 font-medium text-white hover:bg-gray-700"
          >
            {isCreateOpen ? "Cancelar" : "Novo produto"}
          </button>
        </div>
      </div>

      {error && <p className="mt-4 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      {isCreateOpen && (
        <form onSubmit={handleCreateSubmit} className="mt-6 space-y-4 rounded border p-4">
          <h2 className="text-lg font-medium">Novo produto</h2>
          <ProductFields form={productForm} onChange={setProductForm} />

          <div>
            <h3 className="text-sm font-semibold">Variações</h3>
            <div className="mt-2 space-y-3">
              {variantForms.map((form, index) => (
                <VariantFields
                  key={index}
                  form={form}
                  idPrefix={`novo-${index}`}
                  onChange={(patch) => updateVariantForm(index, patch)}
                  showStatus={false}
                />
              ))}
            </div>
            <button
              type="button"
              onClick={() => setVariantForms((current) => [...current, { ...EMPTY_VARIANT_FORM }])}
              className="mt-2 text-sm underline"
            >
              + Adicionar outra variação
            </button>
          </div>

          <button
            type="submit"
            disabled={isSaving}
            className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
          >
            {isSaving ? "Salvando..." : "Criar produto"}
          </button>
        </form>
      )}

      <ul className="mt-6 space-y-3">
        {products === null && <p className="text-sm text-gray-500">Carregando...</p>}
        {products?.map((product) => (
          <li key={product.id} className="rounded border p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">{product.nome}</p>
                <p className="text-xs text-gray-500">
                  {product.categoria} · {product.variacoes.length} variação(ões) · {product.status}
                </p>
              </div>
              <div className="flex gap-3 text-sm">
                <button type="button" onClick={() => setExpandedId((id) => (id === product.id ? null : product.id))} className="underline">
                  {expandedId === product.id ? "Fechar" : "Editar"}
                </button>
                {product.status === "ATIVO" && (
                  <button type="button" onClick={() => handleDeactivate(product.id)} className="text-red-600 underline">
                    Inativar
                  </button>
                )}
              </div>
            </div>

            {expandedId === product.id && (
              <ProductEditor
                product={product}
                onSaved={loadProducts}
                onError={(message) => setError(message)}
              />
            )}
          </li>
        ))}
      </ul>
    </main>
  );
}

function ProductFields({ form, onChange }: { form: ProductInput; onChange: (form: ProductInput) => void }) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <div className="col-span-2">
        <label htmlFor="p-nome" className="block text-sm font-medium">Nome</label>
        <input id="p-nome" required value={form.nome} onChange={(e) => onChange({ ...form, nome: e.target.value })}
               className="mt-1 w-full rounded border px-3 py-2 text-sm" />
      </div>
      <div className="col-span-2">
        <label htmlFor="p-descricao" className="block text-sm font-medium">Descrição</label>
        <textarea id="p-descricao" value={form.descricao} onChange={(e) => onChange({ ...form, descricao: e.target.value })}
                   rows={2} className="mt-1 w-full rounded border px-3 py-2 text-sm" />
      </div>
      <div>
        <label htmlFor="p-categoria" className="block text-sm font-medium">Categoria</label>
        <input id="p-categoria" required value={form.categoria} onChange={(e) => onChange({ ...form, categoria: e.target.value })}
               className="mt-1 w-full rounded border px-3 py-2 text-sm" />
      </div>
      <div>
        <label htmlFor="p-marca" className="block text-sm font-medium">Marca</label>
        <input id="p-marca" value={form.marca} onChange={(e) => onChange({ ...form, marca: e.target.value })}
               className="mt-1 w-full rounded border px-3 py-2 text-sm" />
      </div>
      <div>
        <label htmlFor="p-modalidade" className="block text-sm font-medium">Modalidade</label>
        <input id="p-modalidade" value={form.modalidade} onChange={(e) => onChange({ ...form, modalidade: e.target.value })}
               className="mt-1 w-full rounded border px-3 py-2 text-sm" />
      </div>
    </div>
  );
}

function VariantFields({ form, idPrefix, onChange, showStatus }: {
  form: VariantFormState;
  idPrefix: string;
  onChange: (patch: Partial<VariantFormState>) => void;
  showStatus: boolean;
}) {
  return (
    <div className="grid grid-cols-3 gap-2 rounded border p-2 text-sm">
      <div>
        <label htmlFor={`${idPrefix}-sku`} className="block text-xs font-medium">SKU</label>
        <input id={`${idPrefix}-sku`} required value={form.sku} onChange={(e) => onChange({ sku: e.target.value })}
               className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-tamanho`} className="block text-xs font-medium">Tamanho</label>
        <input id={`${idPrefix}-tamanho`} value={form.tamanho} onChange={(e) => onChange({ tamanho: e.target.value })}
               className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-cor`} className="block text-xs font-medium">Cor</label>
        <input id={`${idPrefix}-cor`} value={form.cor} onChange={(e) => onChange({ cor: e.target.value })}
               className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-preco`} className="block text-xs font-medium">Preço</label>
        <input id={`${idPrefix}-preco`} required type="number" step="0.01" value={form.preco}
               onChange={(e) => onChange({ preco: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-estoque`} className="block text-xs font-medium">Estoque inicial</label>
        <input id={`${idPrefix}-estoque`} type="number" value={form.estoqueDisponivel}
               onChange={(e) => onChange({ estoqueDisponivel: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      {showStatus && (
        <div>
          <label htmlFor={`${idPrefix}-status`} className="block text-xs font-medium">Status</label>
          <select id={`${idPrefix}-status`} value={form.status} onChange={(e) => onChange({ status: e.target.value })}
                  className="mt-1 w-full rounded border px-2 py-1">
            <option value="DISPONIVEL">Disponível</option>
            <option value="ESGOTADO">Esgotado</option>
            <option value="DESCONTINUADO">Descontinuado</option>
          </select>
        </div>
      )}
      <div>
        <label htmlFor={`${idPrefix}-peso`} className="block text-xs font-medium">Peso (kg)</label>
        <input id={`${idPrefix}-peso`} required type="number" step="0.001" value={form.pesoKg}
               onChange={(e) => onChange({ pesoKg: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-altura`} className="block text-xs font-medium">Altura (cm)</label>
        <input id={`${idPrefix}-altura`} required type="number" step="0.01" value={form.alturaCm}
               onChange={(e) => onChange({ alturaCm: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-largura`} className="block text-xs font-medium">Largura (cm)</label>
        <input id={`${idPrefix}-largura`} required type="number" step="0.01" value={form.larguraCm}
               onChange={(e) => onChange({ larguraCm: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-comprimento`} className="block text-xs font-medium">Comprimento (cm)</label>
        <input id={`${idPrefix}-comprimento`} required type="number" step="0.01" value={form.comprimentoCm}
               onChange={(e) => onChange({ comprimentoCm: e.target.value })} className="mt-1 w-full rounded border px-2 py-1" />
      </div>
    </div>
  );
}

function variantToForm(variant: Variant): VariantFormState {
  const atributos = variant.atributos as Record<string, unknown>;
  return {
    sku: variant.sku,
    tamanho: String(atributos.tamanho ?? ""),
    cor: String(atributos.cor ?? ""),
    preco: String(variant.preco),
    estoqueDisponivel: String(variant.estoqueDisponivel),
    pesoKg: "1.000",
    alturaCm: "15.00",
    larguraCm: "30.00",
    comprimentoCm: "90.00",
    status: variant.status,
  };
}

function ProductEditor({ product, onSaved, onError }: {
  product: ProductDetail;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const [form, setForm] = useState<ProductInput>({
    nome: product.nome,
    descricao: product.descricao ?? "",
    categoria: product.categoria,
    marca: product.marca ?? "",
    modalidade: product.modalidade ?? "",
    especificacoesTecnicas: product.especificacoesTecnicas,
    tabelaGeometria: product.tabelaGeometria,
    imagens: product.imagens,
  });
  const [isSaving, setIsSaving] = useState(false);
  const [editingVariantId, setEditingVariantId] = useState<number | null>(null);
  const [variantForm, setVariantForm] = useState<VariantFormState>(EMPTY_VARIANT_FORM);
  const [isAddingVariant, setIsAddingVariant] = useState(false);
  const [newVariantForm, setNewVariantForm] = useState<VariantFormState>({ ...EMPTY_VARIANT_FORM });

  async function handleProductSave(event: React.FormEvent) {
    event.preventDefault();
    setIsSaving(true);
    try {
      await updateProduct(product.id, form);
      onSaved();
    } catch (err) {
      onError(err instanceof ApiRequestError ? err.error.message : "Não foi possível salvar o produto agora.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleVariantSave(variantId: number) {
    try {
      await updateVariant(product.id, variantId, toUpdateVariantInput(variantForm));
      setEditingVariantId(null);
      onSaved();
    } catch (err) {
      onError(err instanceof ApiRequestError ? err.error.message : "Não foi possível salvar a variação agora.");
    }
  }

  async function handleAddVariant(event: React.FormEvent) {
    event.preventDefault();
    try {
      await addVariant(product.id, toVariantInput(newVariantForm));
      setIsAddingVariant(false);
      setNewVariantForm({ ...EMPTY_VARIANT_FORM });
      onSaved();
    } catch (err) {
      onError(err instanceof ApiRequestError ? err.error.message : "Não foi possível adicionar a variação agora.");
    }
  }

  return (
    <div className="mt-4 space-y-4 border-t pt-4">
      <form onSubmit={handleProductSave} className="space-y-3">
        <ProductFields form={form} onChange={setForm} />
        <button type="submit" disabled={isSaving}
                className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300">
          {isSaving ? "Salvando..." : "Salvar produto"}
        </button>
      </form>

      <div>
        <h3 className="text-sm font-semibold">Variações</h3>
        <ul className="mt-2 space-y-2">
          {product.variacoes.map((variant) => (
            <li key={variant.id} className="rounded border p-2 text-sm">
              {editingVariantId === variant.id ? (
                <div className="space-y-2">
                  <VariantFields form={variantForm} idPrefix={`edit-${variant.id}`}
                                  onChange={(patch) => setVariantForm((f) => ({ ...f, ...patch }))} showStatus />
                  <div className="flex gap-2">
                    <button type="button" onClick={() => handleVariantSave(variant.id)}
                            className="rounded bg-gray-900 px-3 py-1 text-xs font-medium text-white">Salvar</button>
                    <button type="button" onClick={() => setEditingVariantId(null)}
                            className="rounded border px-3 py-1 text-xs">Cancelar</button>
                  </div>
                </div>
              ) : (
                <div className="flex items-center justify-between">
                  <span>{variant.sku} — R$ {variant.preco.toFixed(2)} — estoque {variant.estoqueDisponivel} — {variant.status}</span>
                  <button type="button" onClick={() => { setEditingVariantId(variant.id); setVariantForm(variantToForm(variant)); }}
                          className="underline">Editar</button>
                </div>
              )}
            </li>
          ))}
        </ul>

        {isAddingVariant ? (
          <form onSubmit={handleAddVariant} className="mt-2 space-y-2">
            <VariantFields form={newVariantForm} idPrefix="nova-variacao"
                            onChange={(patch) => setNewVariantForm((f) => ({ ...f, ...patch }))} showStatus={false} />
            <div className="flex gap-2">
              <button type="submit" className="rounded bg-gray-900 px-3 py-1 text-xs font-medium text-white">Adicionar</button>
              <button type="button" onClick={() => setIsAddingVariant(false)} className="rounded border px-3 py-1 text-xs">Cancelar</button>
            </div>
          </form>
        ) : (
          <button type="button" onClick={() => setIsAddingVariant(true)} className="mt-2 text-sm underline">
            + Adicionar variação
          </button>
        )}
      </div>
    </div>
  );
}
