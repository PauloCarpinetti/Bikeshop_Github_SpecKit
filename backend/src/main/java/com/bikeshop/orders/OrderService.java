package com.bikeshop.orders;

import com.bikeshop.checkout.shipping.ShippingQuote;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.common.messaging.DomainEventPublisher;
import com.bikeshop.orders.dto.OrderDto;
import com.bikeshop.orders.dto.OrderItemDto;
import com.bikeshop.orders.dto.OrderStatusHistoryEntryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação de pedido a partir do carrinho e transições de status (FR-007). Preço de cada item é
 * congelado no momento da criação (snapshot em {@link ItemPedido}, independente de mudanças
 * futuras no preço da variação).
 */
@Service
@Transactional
public class OrderService {

    private static final Set<PedidoStatus> STATUS_TERMINAIS = Set.of(PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ObjectMapper objectMapper, DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public Pedido criarPedido(String cartId, Long clienteId, String clienteNome, String clienteEmail,
                               EnderecoEntregaInput endereco, List<OrderLineItemInput> itens, ShippingQuote frete) {
        if (itens.isEmpty()) {
            throw new BusinessException("CARRINHO_VAZIO", "Não é possível criar um pedido sem itens", HttpStatus.BAD_REQUEST);
        }

        BigDecimal valorItens = itens.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = new Pedido(
                cartId, clienteId, clienteNome, clienteEmail,
                toJson(endereco),
                valorItens, frete.valor(), frete.transportadora(), frete.prazoDias(),
                historicoInicial()
        );

        for (OrderLineItemInput item : itens) {
            pedido.addItem(new ItemPedido(pedido, item.variacaoProdutoId(), item.sku(), item.nomeProduto(),
                    item.precoUnitario(), item.quantidade()));
        }

        return orderRepository.save(pedido);
    }

    public Pedido buscarPorId(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Pedido", id));
    }

    public java.util.Optional<Pedido> findByPaymentReference(String paymentReference) {
        return orderRepository.findByPaymentReference(paymentReference);
    }

    public Pedido atualizarStatus(Long pedidoId, PedidoStatus novoStatus) {
        Pedido pedido = buscarPorId(pedidoId);
        PedidoStatus statusAnterior = pedido.getStatus();
        validarTransicao(statusAnterior, novoStatus);
        pedido.setStatus(novoStatus);
        pedido.setStatusHistorico(comHistoricoAdicionado(pedido.getStatusHistorico(), novoStatus));

        // A transição inicial (CRIADO -> AGUARDANDO_PAGAMENTO) ocorre dentro do próprio checkout,
        // que já publica OrderCreatedEvent — notificar de novo aqui seria redundante (T066).
        boolean transicaoInicialDoCheckout = statusAnterior == PedidoStatus.CRIADO && novoStatus == PedidoStatus.AGUARDANDO_PAGAMENTO;
        if (!transicaoInicialDoCheckout) {
            eventPublisher.publish("orders.status-changed", new OrderStatusChangedEvent(
                    pedido.getId(), pedido.getClienteNome(), pedido.getClienteEmail(),
                    statusAnterior.name(), novoStatus.name()));
        }

        return pedido;
    }

    public Pedido aplicarCupom(Long pedidoId, String cupomCodigo, java.math.BigDecimal valorDesconto) {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.aplicarCupom(cupomCodigo, valorDesconto);
        return pedido;
    }

    public Pedido registrarPagamento(Long pedidoId, String provider, String reference, String status) {
        Pedido pedido = buscarPorId(pedidoId);
        pedido.setPayment(provider, reference, status);
        return pedido;
    }

    public OrderDto toDto(Pedido pedido) {
        List<OrderItemDto> itens = pedido.getItens().stream()
                .map(item -> new OrderItemDto(item.getVariacaoProdutoId(), item.getSku(), item.getNomeProduto(),
                        item.getPrecoUnitario(), item.getQuantidade(), item.getSubtotal()))
                .toList();

        return new OrderDto(
                pedido.getId(), pedido.getClienteNome(), pedido.getClienteEmail(),
                pedido.getStatus().name(), pedido.getValorItens(), pedido.getValorFrete(),
                pedido.getValorDesconto(), pedido.getCupomCodigo(), pedido.getValorTotal(),
                pedido.getTransportadora(), pedido.getPrazoFreteDias(),
                pedido.getPaymentProvider(), pedido.getPaymentReference(), pedido.getPaymentStatus(),
                pedido.getCriadoEm(), toStatusHistorico(pedido.getStatusHistorico()),
                toEndereco(pedido.getEnderecoEntrega()), itens
        );
    }

    private List<OrderStatusHistoryEntryDto> toStatusHistorico(String statusHistoricoJson) {
        try {
            ArrayNode array = (ArrayNode) objectMapper.readTree(statusHistoricoJson);
            List<OrderStatusHistoryEntryDto> historico = new java.util.ArrayList<>();
            array.forEach(node -> historico.add(new OrderStatusHistoryEntryDto(
                    node.get("status").asText(), Instant.parse(node.get("timestamp").asText()))));
            return historico;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private EnderecoEntregaInput toEndereco(String enderecoJson) {
        try {
            return objectMapper.readValue(enderecoJson, EnderecoEntregaInput.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void validarTransicao(PedidoStatus atual, PedidoStatus novo) {
        // ENTREGUE é terminal para o fluxo de venda, mas ainda permite a ramificação de
        // pós-venda para troca/devolução (data-model.md, FR-008/US2) — só bloqueia demais saídas.
        boolean ramificacaoDeTrocaDevolucao = atual == PedidoStatus.ENTREGUE && novo == PedidoStatus.EM_TROCA_DEVOLUCAO;
        if (STATUS_TERMINAIS.contains(atual) && !ramificacaoDeTrocaDevolucao) {
            throw new BusinessException(
                    "TRANSICAO_INVALIDA",
                    "Pedido em status terminal (%s) não pode mudar para %s".formatted(atual, novo),
                    HttpStatus.CONFLICT
            );
        }
    }

    private String toJson(EnderecoEntregaInput endereco) {
        try {
            return objectMapper.writeValueAsString(endereco);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar endereço de entrega", ex);
        }
    }

    private String historicoInicial() {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(historicoEntrada(PedidoStatus.CRIADO));
        return array.toString();
    }

    private String comHistoricoAdicionado(String historicoAtualJson, PedidoStatus novoStatus) {
        try {
            ArrayNode array = (ArrayNode) objectMapper.readTree(historicoAtualJson);
            array.add(historicoEntrada(novoStatus));
            return array.toString();
        } catch (Exception ex) {
            ArrayNode array = objectMapper.createArrayNode();
            array.add(historicoEntrada(novoStatus));
            return array.toString();
        }
    }

    private ObjectNode historicoEntrada(PedidoStatus status) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", status.name());
        node.put("timestamp", Instant.now().toString());
        return node;
    }
}
