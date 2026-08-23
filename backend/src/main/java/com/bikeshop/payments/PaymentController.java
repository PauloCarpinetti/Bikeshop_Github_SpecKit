package com.bikeshop.payments;

import com.bikeshop.orders.OrderService;
import com.bikeshop.orders.Pedido;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.orders.dto.OrderDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de pagamento (FR-006). Ver contracts/api-overview.md.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final OrderService orderService;
    private final PaymentGatewayResolver paymentGatewayResolver;

    public PaymentController(OrderService orderService, PaymentGatewayResolver paymentGatewayResolver) {
        this.orderService = orderService;
        this.paymentGatewayResolver = paymentGatewayResolver;
    }

    @PostMapping("/{orderId}/intents")
    public OrderDto createIntent(@PathVariable Long orderId, @Valid @RequestBody CreatePaymentIntentRequest request) {
        Pedido pedido = orderService.buscarPorId(orderId);
        PaymentGatewayAdapter adapter = paymentGatewayResolver.resolve(request.provider());

        PaymentIntentResult intent = adapter.createIntent(pedido);
        orderService.registrarPagamento(orderId, request.provider().name(), intent.reference(), intent.status());
        orderService.atualizarStatus(orderId, PedidoStatus.AGUARDANDO_PAGAMENTO);

        return orderService.toDto(orderService.buscarPorId(orderId));
    }

    @PostMapping("/webhooks/{provider}")
    public void receiveWebhook(@PathVariable String provider, @RequestBody String rawPayload, HttpServletRequest request) {
        PaymentGatewayAdapter adapter = paymentGatewayResolver.resolveByPathSegment(provider);
        PaymentWebhookEvent event = adapter.parseWebhook(rawPayload, headersOf(request));

        orderService.findByPaymentReference(event.reference()).ifPresent(pedido -> {
            orderService.registrarPagamento(pedido.getId(), adapter.getProvider().name(), event.reference(), event.status());
            PedidoStatus novoStatus = mapStatus(event.status());
            if (novoStatus != null) {
                orderService.atualizarStatus(pedido.getId(), novoStatus);
            }
        });
    }

    private PedidoStatus mapStatus(String gatewayStatus) {
        if (gatewayStatus == null) {
            return null;
        }
        String normalized = gatewayStatus.toLowerCase(Locale.ROOT);
        if (normalized.contains("approved") || normalized.contains("succeeded") || normalized.contains("paid")) {
            return PedidoStatus.PAGO;
        }
        if (normalized.contains("reject") || normalized.contains("declin") || normalized.contains("fail") || normalized.contains("cancel")) {
            return PedidoStatus.PAGAMENTO_RECUSADO;
        }
        return null;
    }

    private Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
