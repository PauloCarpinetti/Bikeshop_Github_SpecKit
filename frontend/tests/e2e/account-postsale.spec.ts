import { test, expect } from "@playwright/test";

/**
 * Cenário 2 de quickstart.md (conta, pedidos e pós-venda, User Story 2): cadastro → compra →
 * atualização de conta/endereço → histórico com rastreamento. As etapas de devolução e avaliação
 * só ficam disponíveis quando o pedido está ENTREGUE — status hoje só alcançável via backoffice
 * (Fase 5, ainda não implementada), por isso são validadas via teste de contrato no backend
 * (PostSaleContractTest, que avança o status diretamente pelo OrderService) em vez de E2E aqui.
 */
test("cliente atualiza conta, cadastra endereço e acompanha o pedido no histórico", async ({ page }) => {
  const email = `postsale-e2e-${Date.now()}@example.com`;

  await page.goto("/register");
  await page.getByLabel("Nome completo").fill("Cliente PosVenda E2E");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill("senha12345");
  await page.getByRole("button", { name: "Criar conta" }).click();
  await expect(page).toHaveURL(/\/products/);

  // Monta um pedido para aparecer no histórico.
  await page.getByRole("link", { name: /Capacete Ciclista ProSafe/ }).click();
  await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
  await expect(page.getByRole("dialog", { name: "Carrinho de compras" })).toBeVisible();

  await page.goto("/checkout");
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

  // Atualiza dados cadastrais.
  await page.goto("/profile");
  await page.getByLabel("Telefone").fill("11999998888");
  await page.getByRole("button", { name: "Salvar dados" }).click();
  await expect(page.getByText("Dados atualizados.")).toBeVisible();

  // Cadastra um endereço.
  await page.getByRole("button", { name: "Adicionar endereço" }).click();
  await page.getByLabel("CEP").fill("20040-020");
  await page.getByLabel("Estado (UF)").fill("RJ");
  await page.getByLabel("Logradouro").fill("Av. Rio Branco");
  await page.getByLabel("Número").fill("1");
  await page.getByLabel("Bairro").fill("Centro");
  await page.getByLabel("Cidade").fill("Rio de Janeiro");
  await page.getByRole("button", { name: "Salvar endereço" }).click();
  await expect(page.getByText("Av. Rio Branco, 1")).toBeVisible();

  // Histórico e rastreamento do pedido.
  await page.goto("/orders");
  await expect(page.getByText(`Pedido #${pedidoId}`)).toBeVisible();
  await page.getByText(`Pedido #${pedidoId}`).click();
  await expect(page.getByText("Rastreamento")).toBeVisible();
  await expect(page.getByText("Aguardando pagamento")).toBeVisible();
});
