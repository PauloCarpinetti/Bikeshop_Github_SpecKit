package com.bikeshop.admin;

import com.bikeshop.admin.dto.CouponDto;
import com.bikeshop.admin.dto.CreateCouponRequest;
import com.bikeshop.admin.dto.UpdateCouponRequest;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de cupons de desconto pelo backoffice (FR-009, T079).
 */
@Service
@Transactional
public class CouponAdminService {

    private final CupomDescontoRepository cupomDescontoRepository;
    private final ObjectMapper objectMapper;

    public CouponAdminService(CupomDescontoRepository cupomDescontoRepository, ObjectMapper objectMapper) {
        this.cupomDescontoRepository = cupomDescontoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> listar() {
        return cupomDescontoRepository.findAll().stream().map(this::toDto).toList();
    }

    public CouponDto criar(CreateCouponRequest request) {
        if (cupomDescontoRepository.findByCodigoIgnoreCase(request.codigo()).isPresent()) {
            throw new BusinessException("CODIGO_EM_USO", "Já existe um cupom com o código %s".formatted(request.codigo()),
                    HttpStatus.CONFLICT);
        }
        CupomDesconto cupom = new CupomDesconto(request.codigo().toUpperCase(), CupomTipo.valueOf(request.tipo()),
                request.valor(), request.validoDe(), request.validoAte(), request.valorMinimoCarrinho(),
                toJson(request.categoriasAplicaveis()), request.limiteDeUso());
        return toDto(cupomDescontoRepository.save(cupom));
    }

    public CouponDto atualizar(Long id, UpdateCouponRequest request) {
        CupomDesconto cupom = requireCupom(id);
        cupom.atualizar(CupomTipo.valueOf(request.tipo()), request.valor(), request.validoDe(), request.validoAte(),
                request.valorMinimoCarrinho(), toJson(request.categoriasAplicaveis()), request.limiteDeUso());
        return toDto(cupom);
    }

    public void desativar(Long id) {
        requireCupom(id).expirarAgora();
    }

    private CupomDesconto requireCupom(Long id) {
        return cupomDescontoRepository.findById(id).orElseThrow(() -> new NotFoundException("Cupom", id));
    }

    private CouponDto toDto(CupomDesconto cupom) {
        return new CouponDto(cupom.getId(), cupom.getCodigo(), cupom.getTipo().name(), cupom.getValor(),
                cupom.getValidoDe(), cupom.getValidoAte(), cupom.getValorMinimoCarrinho(),
                readCategorias(cupom.getCategoriasAplicaveis()), cupom.getLimiteDeUso(), cupom.getUsosRealizados());
    }

    private String toJson(List<String> categorias) {
        try {
            return objectMapper.writeValueAsString(categorias == null ? List.of() : categorias);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar categorias do cupom", ex);
        }
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
