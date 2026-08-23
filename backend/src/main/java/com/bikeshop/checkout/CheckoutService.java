package com.bikeshop.checkout;

import com.bikeshop.cart.CartService;
import com.bikeshop.cart.dto.CartItemViewDto;
import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.catalog.InventoryAdjustedEvent;
import com.bikeshop.catalog.VariacaoProduto;
import com.bikeshop.catalog.VariacaoProdutoRepository;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.checkout.dto.ShippingQuoteResponseDto;
import com.bikeshop.checkout.shipping.ShippingLineItem;
import com.bikeshop.checkout.shipping.ShippingProvider;
import com.bikeshop.checkout.shipping.ShippingQuote;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.common.messaging.DomainEventPublisher;
import com.bikeshop.orders.OrderCreatedEvent;
import com.bikeshop.orders.OrderLineItemInput;
import com.bikeshop.orders.OrderService;
import com.bikeshop.orders.Pedido;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.payments.PaymentGatewayAdapter;
import com.bikeshop.payments.PaymentGatewayResolver;
import com.bikeshop.payments.PaymentIntentResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestração do checkout (T038): calcula frete, debita estoque, cria o pedido, inicia o
 * pagamento e limpa o carrinho — tudo em uma única transação. Publica os eventos de
 * pedido/estoque (T046) após o commit lógico da orquestração.
 */
@Service
@Transactional
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartService cartService;
    private final VariacaoProdutoRepository variacaoProdutoRepository;
    private final ShippingProvider shippingProvider;
    private final OrderService orderService;
    private final PaymentGatewayResolver paymentGatewayResolver;
    private final DomainEventPublisher eventPublisher;

    public CheckoutService(CartService cartService, VariacaoProdutoRepository variacaoProdutoRepository,
                            ShippingProvider shippingProvider, OrderService orderService,
                            PaymentGatewayResolver paymentGatewayResolver, DomainEventPublisher eventPublisher) {
        this.cartService = cartService;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
        this.shippingProvider = shippingProvider;
        this.orderService = orderService;
        this.paymentGatewayResolver = paymentGatewayResolver;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public ShippingQuoteResponseDto quoteShipping(String cartId, String cep) {
        CartViewDto cart = requireNonEmptyCart(cartId);
        ShippingQuote quote = shippingProvider.calculate(cep, toShippingLineItems(cart));
        return new ShippingQuoteResponseDto(quote.transportadora(), quote.valor(), quote.prazoDias(), quote.estimado());
    }

    public CheckoutResultDto criarPedido(String cartId, CreateOrderRequest request) {
        CartViewDto cart = requireNonEmptyCart(cartId);
        log.info("Iniciando checkout: cartId={} itens={} paymentProvider={}",
                cartId, cart.itens().size(), request.paymentProvider());

        ShippingQuote frete = shippingProvider.calculate(request.endereco().cep(), toShippingLineItems(cart));
        log.info("Frete calculado: cartId={} transportadora={} valor={} estimado={}",
                cartId, frete.transportadora(), frete.valor(), frete.estimado());

        // Debita o estoque de cada item (entidades gerenciadas nesta transação — persistido no commit)
        // e publica um evento de estoque por item debitado.
        for (CartItemViewDto item : cart.itens()) {
            VariacaoProduto variacao = buscarVariacao(item.variacaoProdutoId());
            variacao.debitarEstoque(item.quantidade());
            eventPublisher.publish("inventory.adjusted", new InventoryAdjustedEvent(
                    variacao.getId(), variacao.getSku(), item.quantidade(), variacao.getEstoqueDisponivel()));
        }

        List<OrderLineItemInput> itensPedido = cart.itens().stream()
                .map(item -> new OrderLineItemInput(item.variacaoProdutoId(), item.sku(), item.nomeProduto(),
                        item.precoUnitario(), item.quantidade()))
                .toList();

        Pedido pedido = orderService.criarPedido(cartId, request.clienteNome(), request.clienteEmail(),
                request.endereco(), itensPedido, frete);

        PaymentGatewayAdapter adapter = paymentGatewayResolver.resolve(request.paymentProvider());
        PaymentIntentResult intent = adapter.createIntent(pedido);
        orderService.registrarPagamento(pedido.getId(), request.paymentProvider().name(), intent.reference(), intent.status());
        Pedido atualizado = orderService.atualizarStatus(pedido.getId(), PedidoStatus.AGUARDANDO_PAGAMENTO);

        eventPublisher.publish("orders.created",
                new OrderCreatedEvent(pedido.getId(), pedido.getClienteNome(), pedido.getClienteEmail()));

        cartService.clear(cartId);

        log.info("Checkout concluído: pedidoId={} status={} valorTotal={} paymentProvider={} paymentSimulado={}",
                pedido.getId(), atualizado.getStatus(), atualizado.getValorTotal(), request.paymentProvider(), intent.simulado());

        return new CheckoutResultDto(orderService.toDto(atualizado), intent.redirectUrl(), intent.simulado());
    }

    private CartViewDto requireNonEmptyCart(String cartId) {
        CartViewDto cart = cartService.getCartView(cartId);
        if (cart.itens().isEmpty()) {
            throw new BusinessException("CARRINHO_VAZIO", "Adicione itens ao carrinho antes de continuar", HttpStatus.BAD_REQUEST);
        }
        return cart;
    }

    private List<ShippingLineItem> toShippingLineItems(CartViewDto cart) {
        return cart.itens().stream()
                .map(item -> {
                    VariacaoProduto variacao = buscarVariacao(item.variacaoProdutoId());
                    return new ShippingLineItem(variacao.getPesoKg(), variacao.getAlturaCm(),
                            variacao.getLarguraCm(), variacao.getComprimentoCm(), item.quantidade());
                })
                .toList();
    }

    private VariacaoProduto buscarVariacao(Long id) {
        return variacaoProdutoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variação de produto", id));
    }
}
