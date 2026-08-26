import { test, expect } from "@playwright/test";

/**
 * Cenário 3 de quickstart.md (backoffice, User Story 3, T071): produto → estoque → pedido → cupom.
 * Usa o pedido criado por um cliente convidado para validar a atualização de status pelo admin —
 * o mesmo padrão de OrderAdminContractTest no backend. Login usa o usuário administrativo semeado
 * por AdminUserSeeder (Fase 5A), único jeito de obter um papel ADMIN/OPERATOR hoje.
 */
test("admin cadastra produto, ajusta estoque, atualiza pedido e cria cupom", async ({ page }) => {
  const suffix = Date.now();

  // Pedido de um cliente convidado, para o admin atualizar o status mais adiante.
  await page.goto("/products");
  await page.getByRole("link", { name: /Capacete Ciclista ProSafe/ }).click();
  await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
  await expect(page.getByRole("dialog", { name: "Carrinho de compras" })).toBeVisible();

  await page.goto("/checkout");
  await page.getByLabel("Nome completo").fill("Cliente Convidado E2E");
  await page.getByLabel("E-mail").fill(`convidado-${suffix}@example.com`);
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
  const pedidoText = await page.getByText(/Pedido #\d+/).textContent();
  const pedidoId = pedidoText?.match(/#(\d+)/)?.[1];

  // Login administrativo (usuário semeado em dev, Fase 5A).
  await page.goto("/login");
  await page.getByLabel("E-mail").fill("admin@bikeshop.example");
  await page.getByLabel("Senha").fill("admin12345");
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/products/);

  // Produto: cadastro com uma variação.
  const productName = `Bike E2E Backoffice ${suffix}`;
  const sku = `E2E-${suffix}`;
  await page.goto("/admin/products");
  await page.getByRole("button", { name: "Novo produto" }).click();
  await page.getByLabel("Nome").fill(productName);
  await page.getByLabel("Categoria").fill("Bicicleta");
  await page.getByLabel("SKU").fill(sku);
  await page.getByLabel("Preço").fill("1500.00");
  await page.getByRole("button", { name: "Criar produto" }).click();
  await expect(page.getByText(productName)).toBeVisible();

  // Estoque: ajuste da variação recém-criada.
  await page.goto("/admin/inventory");
  const inventoryCard = page.locator("div", { hasText: productName }).last();
  await inventoryCard.getByLabel(`Ajuste de estoque para ${sku}`).fill("5");
  await inventoryCard.getByRole("button", { name: "Aplicar" }).click();
  await expect(page.getByText(`Estoque de ${sku} atualizado para 5.`)).toBeVisible();

  // Pedido: atualização de status do pedido do cliente convidado.
  await page.goto("/admin/orders");
  const orderRow = page.locator("li", { hasText: `Pedido #${pedidoId}` });
  await orderRow.getByLabel(`Novo status para o pedido #${pedidoId}`).selectOption("PAGO");
  await orderRow.getByRole("button", { name: "Atualizar status" }).click();
  await expect(orderRow.getByText("status atual: PAGO", { exact: false })).toBeVisible();

  // Cupom: criação de um cupom de desconto.
  const codigo = `E2E${suffix}`;
  await page.goto("/admin/coupons");
  await page.getByRole("button", { name: "Novo cupom" }).click();
  await page.getByLabel("Código").fill(codigo);
  await page.getByRole("button", { name: "Criar cupom" }).click();
  await expect(page.getByText(codigo, { exact: false })).toBeVisible();
});
