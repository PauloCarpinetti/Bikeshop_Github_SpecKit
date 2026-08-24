package com.bikeshop.checkout;

import com.bikeshop.admin.CupomDesconto;
import com.bikeshop.admin.CupomDescontoRepository;
import com.bikeshop.admin.CupomTipo;
import com.bikeshop.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validação e aplicação de cupom de desconto no checkout (FR-009). Cupons são criados no
 * backoffice ({@link com.bikeshop.admin.CouponAdminService}); esta classe só consome.
 */
@Service
@Transactional
public class CouponService {

    private final CupomDescontoRepository cupomDescontoRepository;
    private final ObjectMapper objectMapper;

    public CouponService(CupomDescontoRepository cupomDescontoRepository, ObjectMapper objectMapper) {
        this.cupomDescontoRepository = cupomDescontoRepository;
        this.objectMapper = objectMapper;
    }

    /** Valida o cupom contra o carrinho (edge case da spec: cupom expirado/esgotado/incompatível
     * MUST ser rejeitado com mensagem clara) e calcula o desconto, sem registrar o uso ainda. */
    public CouponValidationResult validar(String codigo, BigDecimal valorItens, List<String> categoriasNoCarrinho) {
        CupomDesconto cupom = cupomDescontoRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new BusinessException("CUPOM_INVALIDO", "Cupom não encontrado", HttpStatus.NOT_FOUND));

        Instant agora = Instant.now();
        if (agora.isBefore(cupom.getValidoDe()) || agora.isAfter(cupom.getValidoAte())) {
            throw new BusinessException("CUPOM_EXPIRADO", "Este cupom não está mais válido", HttpStatus.CONFLICT);
        }
        if (cupom.getLimiteDeUso() != null && cupom.getUsosRealizados() >= cupom.getLimiteDeUso()) {
            throw new BusinessException("CUPOM_ESGOTADO", "Este cupom já atingiu o limite de uso", HttpStatus.CONFLICT);
        }
        if (cupom.getValorMinimoCarrinho() != null && valorItens.compareTo(cupom.getValorMinimoCarrinho()) < 0) {
            throw new BusinessException("CUPOM_VALOR_MINIMO",
                    "O carrinho precisa somar pelo menos %s para usar este cupom".formatted(cupom.getValorMinimoCarrinho()),
                    HttpStatus.CONFLICT);
        }
        List<String> categoriasPermitidas = readCategorias(cupom.getCategoriasAplicaveis());
        if (!categoriasPermitidas.isEmpty() && categoriasNoCarrinho.stream().noneMatch(categoriasPermitidas::contains)) {
            throw new BusinessException("CUPOM_CATEGORIA_INCOMPATIVEL",
                    "Este cupom não é aplicável aos itens do carrinho", HttpStatus.CONFLICT);
        }

        return new CouponValidationResult(cupom, calcularDesconto(cupom, valorItens));
    }

    public void registrarUso(CupomDesconto cupom) {
        cupom.registrarUso();
    }

    private BigDecimal calcularDesconto(CupomDesconto cupom, BigDecimal valorItens) {
        BigDecimal desconto = cupom.getTipo() == CupomTipo.PERCENTUAL
                ? valorItens.multiply(cupom.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : cupom.getValor();
        return desconto.min(valorItens);
    }

    private List<String> readCategorias(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
