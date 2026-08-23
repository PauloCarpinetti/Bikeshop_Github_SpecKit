import { test, expect } from "@playwright/test";

/**
 * Cenário 1 completo de quickstart.md: catálogo → carrinho → frete → checkout → login com merge
 * de carrinho. Cobre a User Story 1 (P1) de ponta a ponta contra o backend real (dev, seed data).
 */
test("visitante navega, monta carrinho, cria conta (merge) e finaliza a compra", async ({ page }) => {
  await page.goto("/products");
  await expect(page.getByRole("heading", { name: "Catálogo" })).toBeVisible();

  await page.getByRole("link", { name: /Mountain Bike Aro 29 Explorer/ }).click();
  await expect(page.getByRole("heading", { name: "Mountain Bike Aro 29 Explorer" })).toBeVisible();

  await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
  await expect(page.getByRole("dialog", { name: "Carrinho de compras" })).toBeVisible();
  await expect(page.getByText("Mountain Bike Aro 29 Explorer", { exact: false })).toBeVisible();

  // Login/cadastro durante a compra: o carrinho de visitante deve ser preservado (FR-004).
  await page.goto("/register");
  const email = `e2e-${Date.now()}@example.com`;
  await page.getByLabel("Nome completo").fill("E2E Test");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill("senha12345");
  await page.getByRole("button", { name: "Criar conta" }).click();
  await expect(page).toHaveURL(/\/products/);
  await expect(page.getByText("Olá, E2E", { exact: false })).toBeVisible();

  await page.goto("/checkout");
  await expect(page.getByText("Mountain Bike Aro 29 Explorer", { exact: false })).toBeVisible();

  await page.getByLabel("CEP").fill("01310-100");
  await page.getByRole("button", { name: "Calcular frete" }).click();
  await expect(page.getByText("Correios", { exact: false })).toBeVisible();

  await page.getByLabel("Estado (UF)").fill("SP");
  await page.getByLabel("Logradouro").fill("Av. Paulista");
  await page.getByLabel("Número").fill("1000");
  await page.getByLabel("Bairro").fill("Bela Vista");
  await page.getByLabel("Cidade").fill("São Paulo");

  await page.getByRole("button", { name: "Finalizar pedido" }).click();
  await expect(page.getByText("Pedido criado com sucesso!")).toBeVisible();
});
