package com.bikeshop.reviews;

import com.bikeshop.catalog.VariacaoProduto;
import com.bikeshop.catalog.VariacaoProdutoRepository;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.orders.OrderRepository;
import com.bikeshop.orders.Pedido;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.reviews.dto.CreateReviewRequest;
import com.bikeshop.reviews.dto.ReviewAdminDto;
import com.bikeshop.reviews.dto.ReviewDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicação de avaliação de produto (US2): só permite avaliar um item de um pedido do próprio
 * cliente confirmado como entregue (data-model.md), e apenas uma vez por produto/pedido.
 */
@Service
@Transactional
public class ReviewService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final OrderRepository orderRepository;
    private final VariacaoProdutoRepository variacaoProdutoRepository;

    public ReviewService(AvaliacaoRepository avaliacaoRepository, OrderRepository orderRepository,
                          VariacaoProdutoRepository variacaoProdutoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.orderRepository = orderRepository;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
    }

    public ReviewDto publicar(Long clienteId, CreateReviewRequest request) {
        Pedido pedido = orderRepository.findByIdAndClienteId(request.pedidoId(), clienteId)
                .orElseThrow(() -> new NotFoundException("Pedido", request.pedidoId()));

        if (pedido.getStatus() != PedidoStatus.ENTREGUE) {
            throw new BusinessException("PEDIDO_NAO_ENTREGUE",
                    "Só é possível avaliar produtos de pedidos já entregues", HttpStatus.CONFLICT);
        }

        boolean itemPertenceAoPedido = pedido.getItens().stream()
                .anyMatch(item -> item.getVariacaoProdutoId().equals(request.variacaoProdutoId()));
        if (!itemPertenceAoPedido) {
            throw new BusinessException("ITEM_NAO_PERTENCE_AO_PEDIDO",
                    "Este item não faz parte do pedido informado", HttpStatus.BAD_REQUEST);
        }

        VariacaoProduto variacao = variacaoProdutoRepository.findById(request.variacaoProdutoId())
                .orElseThrow(() -> new NotFoundException("Variação de produto", request.variacaoProdutoId()));
        Long produtoId = variacao.getProduto().getId();

        if (avaliacaoRepository.existsByClienteIdAndProdutoIdAndPedidoId(clienteId, produtoId, pedido.getId())) {
            throw new BusinessException("AVALIACAO_JA_EXISTE",
                    "Você já avaliou este produto para este pedido", HttpStatus.CONFLICT);
        }

        Avaliacao avaliacao = avaliacaoRepository.save(
                new Avaliacao(produtoId, clienteId, pedido.getId(), request.nota(), request.comentario()));
        return toDto(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<ReviewAdminDto> listarParaModeracao() {
        return avaliacaoRepository.findAllByOrderByCriadoEmDesc().stream().map(this::toAdminDto).toList();
    }

    @Transactional(readOnly = true)
    public String statusAtual(Long id) {
        return buscarAvaliacao(id).getStatus().name();
    }

    public ReviewAdminDto moderar(Long id, boolean aprovado) {
        Avaliacao avaliacao = buscarAvaliacao(id);
        avaliacao.moderar(aprovado ? AvaliacaoStatus.PUBLICADA : AvaliacaoStatus.MODERADA);
        return toAdminDto(avaliacao);
    }

    private Avaliacao buscarAvaliacao(Long id) {
        return avaliacaoRepository.findById(id).orElseThrow(() -> new NotFoundException("Avaliação", id));
    }

    private ReviewDto toDto(Avaliacao avaliacao) {
        return new ReviewDto(avaliacao.getId(), avaliacao.getProdutoId(), avaliacao.getPedidoId(),
                avaliacao.getNota(), avaliacao.getComentario(), avaliacao.getStatus().name(), avaliacao.getCriadoEm());
    }

    private ReviewAdminDto toAdminDto(Avaliacao avaliacao) {
        return new ReviewAdminDto(avaliacao.getId(), avaliacao.getProdutoId(), avaliacao.getClienteId(),
                avaliacao.getPedidoId(), avaliacao.getNota(), avaliacao.getComentario(), avaliacao.getStatus().name(),
                avaliacao.getCriadoEm());
    }
}
