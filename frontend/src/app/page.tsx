import Link from "next/link";

export default function HomePage() {
  return (
    <main className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-16">
      <h1 className="text-3xl font-semibold">BikeShop</h1>
      <p className="text-gray-600">
        Bicicletas e acessórios para todos os estilos. Confira o catálogo completo.
      </p>
      <Link
        href="/products"
        className="w-fit rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700"
      >
        Ver catálogo
      </Link>
    </main>
  );
}
