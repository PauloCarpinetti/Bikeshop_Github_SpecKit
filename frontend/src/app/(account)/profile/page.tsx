"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";
import { ApiRequestError } from "@/services/apiClient";
import {
  createAddress,
  getProfile,
  listAddresses,
  updateAddress,
  updateProfile,
  type Endereco,
  type EnderecoInput,
  type Profile,
} from "@/services/account";

const EMPTY_ADDRESS_FORM: EnderecoInput = {
  cep: "",
  logradouro: "",
  numero: "",
  complemento: "",
  bairro: "",
  cidade: "",
  estado: "",
  tipo: "ENTREGA",
  padrao: false,
};

export default function ProfilePage() {
  const router = useRouter();
  const { cliente, isLoading: isAuthLoading } = useAuth();

  const [profile, setProfile] = useState<Profile | null>(null);
  const [addresses, setAddresses] = useState<Endereco[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [nome, setNome] = useState("");
  const [telefone, setTelefone] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);

  const [addressForm, setAddressForm] = useState<EnderecoInput>(EMPTY_ADDRESS_FORM);
  const [editingAddressId, setEditingAddressId] = useState<number | null>(null);
  const [isAddressFormOpen, setIsAddressFormOpen] = useState(false);
  const [isSavingAddress, setIsSavingAddress] = useState(false);

  useEffect(() => {
    if (!isAuthLoading && !cliente) {
      router.push("/login");
    }
  }, [isAuthLoading, cliente, router]);

  useEffect(() => {
    if (!cliente) return;
    Promise.all([getProfile(), listAddresses()])
      .then(([profileData, addressesData]) => {
        setProfile(profileData);
        setNome(profileData.nome);
        setTelefone(profileData.telefone ?? "");
        setAddresses(addressesData);
      })
      .catch(() => setError("Não foi possível carregar seus dados agora."))
      .finally(() => setIsLoading(false));
  }, [cliente]);

  async function handleProfileSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSavingProfile(true);
    setError(null);
    setProfileSaved(false);
    try {
      const updated = await updateProfile({
        nome,
        telefone: telefone || undefined,
        novaSenha: novaSenha || undefined,
      });
      setProfile(updated);
      setNovaSenha("");
      setProfileSaved(true);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível salvar o perfil agora.");
    } finally {
      setIsSavingProfile(false);
    }
  }

  function openCreateAddressForm() {
    setAddressForm(EMPTY_ADDRESS_FORM);
    setEditingAddressId(null);
    setIsAddressFormOpen(true);
  }

  function openEditAddressForm(endereco: Endereco) {
    setAddressForm({
      cep: endereco.cep,
      logradouro: endereco.logradouro,
      numero: endereco.numero,
      complemento: endereco.complemento ?? "",
      bairro: endereco.bairro,
      cidade: endereco.cidade,
      estado: endereco.estado,
      tipo: endereco.tipo,
      padrao: endereco.padrao,
    });
    setEditingAddressId(endereco.id);
    setIsAddressFormOpen(true);
  }

  async function handleAddressSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSavingAddress(true);
    setError(null);
    try {
      const saved = editingAddressId
        ? await updateAddress(editingAddressId, addressForm)
        : await createAddress(addressForm);

      setAddresses((current) => {
        const withoutSaved = current.filter((item) => item.id !== saved.id);
        const next = saved.padrao ? withoutSaved.map((item) => ({ ...item, padrao: false })) : withoutSaved;
        return [...next, saved].sort((a, b) => Number(b.padrao) - Number(a.padrao));
      });
      setIsAddressFormOpen(false);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.error.message : "Não foi possível salvar o endereço agora.");
    } finally {
      setIsSavingAddress(false);
    }
  }

  if (isAuthLoading || !cliente || isLoading) {
    return <p className="mx-auto max-w-2xl px-6 py-16 text-sm text-gray-500">Carregando...</p>;
  }

  return (
    <main className="mx-auto max-w-2xl space-y-10 px-6 py-12">
      <div>
        <h1 className="text-2xl font-semibold">Minha conta</h1>
        <p className="mt-1 text-sm text-gray-600">
          <Link href="/orders" className="underline">Ver meus pedidos</Link>
        </p>
      </div>

      {error && <p className="rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <section>
        <h2 className="text-lg font-medium">Dados cadastrais</h2>
        <form onSubmit={handleProfileSubmit} className="mt-4 space-y-4">
          <div>
            <label htmlFor="nome" className="block text-sm font-medium">Nome completo</label>
            <input
              id="nome"
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              required
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="email" className="block text-sm font-medium">E-mail</label>
            <input
              id="email"
              value={profile?.email ?? ""}
              disabled
              className="mt-1 w-full rounded border bg-gray-50 px-3 py-2 text-sm text-gray-500"
            />
          </div>
          <div>
            <label htmlFor="telefone" className="block text-sm font-medium">Telefone</label>
            <input
              id="telefone"
              value={telefone}
              onChange={(event) => setTelefone(event.target.value)}
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="novaSenha" className="block text-sm font-medium">Nova senha (opcional)</label>
            <input
              id="novaSenha"
              type="password"
              value={novaSenha}
              onChange={(event) => setNovaSenha(event.target.value)}
              minLength={8}
              className="mt-1 w-full rounded border px-3 py-2 text-sm"
            />
            <p className="mt-1 text-xs text-gray-500">Deixe em branco para manter a senha atual.</p>
          </div>

          {profileSaved && <p className="text-sm text-green-700">Dados atualizados.</p>}

          <button
            type="submit"
            disabled={isSavingProfile}
            className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300"
          >
            {isSavingProfile ? "Salvando..." : "Salvar dados"}
          </button>
        </form>
      </section>

      <section>
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium">Endereços</h2>
          <button
            type="button"
            onClick={openCreateAddressForm}
            className="rounded border px-3 py-1.5 text-sm font-medium hover:bg-gray-50"
          >
            Adicionar endereço
          </button>
        </div>

        <ul className="mt-4 space-y-3">
          {addresses.length === 0 && !isAddressFormOpen && (
            <p className="text-sm text-gray-500">Nenhum endereço cadastrado ainda.</p>
          )}
          {addresses.map((endereco) => (
            <li key={endereco.id} className="rounded border p-3 text-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p>{endereco.logradouro}, {endereco.numero}{endereco.complemento ? ` — ${endereco.complemento}` : ""}</p>
                  <p className="text-gray-600">{endereco.bairro}, {endereco.cidade} - {endereco.estado} · {endereco.cep}</p>
                  {endereco.padrao && <span className="mt-1 inline-block rounded bg-gray-900 px-2 py-0.5 text-xs text-white">Padrão</span>}
                </div>
                <button
                  type="button"
                  onClick={() => openEditAddressForm(endereco)}
                  className="text-sm underline"
                >
                  Editar
                </button>
              </div>
            </li>
          ))}
        </ul>

        {isAddressFormOpen && (
          <form onSubmit={handleAddressSubmit} className="mt-4 space-y-3 rounded border p-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="addr-cep" className="block text-sm font-medium">CEP</label>
                <input id="addr-cep" required value={addressForm.cep}
                       onChange={(event) => setAddressForm({ ...addressForm, cep: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div>
                <label htmlFor="addr-estado" className="block text-sm font-medium">Estado (UF)</label>
                <input id="addr-estado" required maxLength={2} value={addressForm.estado}
                       onChange={(event) => setAddressForm({ ...addressForm, estado: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div className="col-span-2">
                <label htmlFor="addr-logradouro" className="block text-sm font-medium">Logradouro</label>
                <input id="addr-logradouro" required value={addressForm.logradouro}
                       onChange={(event) => setAddressForm({ ...addressForm, logradouro: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div>
                <label htmlFor="addr-numero" className="block text-sm font-medium">Número</label>
                <input id="addr-numero" required value={addressForm.numero}
                       onChange={(event) => setAddressForm({ ...addressForm, numero: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div>
                <label htmlFor="addr-complemento" className="block text-sm font-medium">Complemento</label>
                <input id="addr-complemento" value={addressForm.complemento}
                       onChange={(event) => setAddressForm({ ...addressForm, complemento: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div className="col-span-2">
                <label htmlFor="addr-bairro" className="block text-sm font-medium">Bairro</label>
                <input id="addr-bairro" required value={addressForm.bairro}
                       onChange={(event) => setAddressForm({ ...addressForm, bairro: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
              <div className="col-span-2">
                <label htmlFor="addr-cidade" className="block text-sm font-medium">Cidade</label>
                <input id="addr-cidade" required value={addressForm.cidade}
                       onChange={(event) => setAddressForm({ ...addressForm, cidade: event.target.value })}
                       className="mt-1 w-full rounded border px-3 py-2 text-sm" />
              </div>
            </div>

            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={addressForm.padrao}
                     onChange={(event) => setAddressForm({ ...addressForm, padrao: event.target.checked })} />
              Definir como endereço padrão
            </label>

            <div className="flex gap-2">
              <button type="submit" disabled={isSavingAddress}
                      className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:bg-gray-300">
                {isSavingAddress ? "Salvando..." : "Salvar endereço"}
              </button>
              <button type="button" onClick={() => setIsAddressFormOpen(false)}
                      className="rounded border px-4 py-2 text-sm font-medium hover:bg-gray-50">
                Cancelar
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}
