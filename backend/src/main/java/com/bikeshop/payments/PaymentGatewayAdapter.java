package com.bikeshop.payments;

import com.bikeshop.orders.Pedido;
import java.util.Map;

/**
 * Ponto único de acoplamento com um gateway de pagamento externo (Strategy). Cada implementação
 * cobre um provedor (Stripe, Mercado Pago, PagSeguro) e é resolvida em runtime pelo
 * {@code CheckoutController}/{@code PaymentController} a partir do {@link PaymentProvider}
 * escolhido no checkout, sem acoplar o domínio de pedidos a nenhum gateway específico
 * (research.md, seção 4).
 */
public interface PaymentGatewayAdapter {

    PaymentProvider getProvider();

    PaymentIntentResult createIntent(Pedido pedido);

    PaymentWebhookEvent parseWebhook(String rawPayload, Map<String, String> headers);
}
