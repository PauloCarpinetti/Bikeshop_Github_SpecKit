import type { Metadata } from "next";
import "./globals.css";
import { CartProvider } from "@/features/cart/CartContext";
import { CartDrawer } from "@/features/cart/CartDrawer";
import { AuthProvider } from "@/features/auth/AuthContext";
import { Header } from "@/components/Header";

export const metadata: Metadata = {
  title: "BikeShop",
  description: "Plataforma de e-commerce de bicicletas e acessórios",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-white text-gray-900 antialiased">
        <AuthProvider>
          <CartProvider>
            <Header />
            {children}
            <CartDrawer />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
