package com.bikeshop.checkout.shipping;

import com.bikeshop.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Transportadora confirmada: Correios (research.md, seção 5). A API oficial dos Correios
 * ({@code https://api.correios.com.br}) exige um usuário/contrato cadastrado para autenticar e
 * calcular preços reais — como ainda não temos essas credenciais, este provider tenta a API real
 * apenas quando {@code CORREIOS_API_USUARIO}/{@code CORREIOS_API_SENHA} estiverem configurados; caso
 * contrário (ambiente atual), cai para uma estimativa local por peso cubado + faixa de CEP, deixada
 * clara na resposta via {@link ShippingQuote#estimado()}. Mesma estratégia de degradação graciosa já
 * usada para o Meilisearch no Foundational.
 */
@Component
public class CorreiosShippingProvider implements ShippingProvider {

    private static final Logger log = LoggerFactory.getLogger(CorreiosShippingProvider.class);
    private static final BigDecimal FATOR_CUBAGEM = new BigDecimal("6000");
    private static final BigDecimal VALOR_BASE = new BigDecimal("18.90");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("4.50");

    private final String usuario;
    private final String senha;
    private final RestClient restClient;

    public CorreiosShippingProvider(@Value("${bikeshop.correios.api-usuario:}") String usuario,
                                     @Value("${bikeshop.correios.api-senha:}") String senha) {
        this.usuario = usuario;
        this.senha = senha;
        this.restClient = RestClient.create("https://api.correios.com.br");
    }

    @Override
    public ShippingQuote calculate(String cepDestino, List<ShippingLineItem> itens) {
        String cep = normalizeCep(cepDestino);

        if (usuario != null && !usuario.isBlank() && senha != null && !senha.isBlank()) {
            try {
                return callRealApi(cep, itens);
            } catch (Exception ex) {
                log.warn("Falha ao consultar a API real dos Correios, usando estimativa local: {}", ex.getMessage());
            }
        }

        return estimateLocally(cep, itens);
    }

    private String normalizeCep(String cepDestino) {
        if (cepDestino == null) {
            throw new BusinessException("CEP_INVALIDO", "CEP de destino é obrigatório", HttpStatus.BAD_REQUEST);
        }
        String digits = cepDestino.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new BusinessException("CEP_INVALIDO", "CEP inválido: %s".formatted(cepDestino), HttpStatus.BAD_REQUEST);
        }
        return digits;
    }

    /**
     * Chamada à API oficial dos Correios. Caminho ainda não validado contra credenciais reais (não
     * disponíveis neste ambiente) — quando o contrato for confirmado, revisar payload/endpoint aqui.
     */
    private ShippingQuote callRealApi(String cep, List<ShippingLineItem> itens) {
        BigDecimal pesoConsiderado = pesoConsiderado(itens);
        // Autenticação OAuth (token) + POST /preco/v1/nacional seguindo o contrato documentado pelos
        // Correios para clientes com contrato ativo.
        var response = restClient.post()
                .uri("/preco/v1/nacional")
                .headers(headers -> headers.setBasicAuth(usuario, senha))
                .body(new CorreiosPriceRequest(cep, pesoConsiderado))
                .retrieve()
                .body(CorreiosPriceResponse.class);

        if (response == null) {
            throw new IllegalStateException("Resposta vazia da API dos Correios");
        }
        return new ShippingQuote("Correios", response.valor(), response.prazoDias(), false);
    }

    private ShippingQuote estimateLocally(String cep, List<ShippingLineItem> itens) {
        BigDecimal pesoConsiderado = pesoConsiderado(itens);
        BigDecimal valor = VALOR_BASE.add(pesoConsiderado.multiply(VALOR_POR_KG)).setScale(2, RoundingMode.HALF_UP);
        int prazoDias = prazoPorRegiao(cep);
        return new ShippingQuote("Correios", valor, prazoDias, true);
    }

    private BigDecimal pesoConsiderado(List<ShippingLineItem> itens) {
        BigDecimal pesoReal = BigDecimal.ZERO;
        BigDecimal pesoCubado = BigDecimal.ZERO;

        for (ShippingLineItem item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            pesoReal = pesoReal.add(item.pesoKg().multiply(quantidade));

            BigDecimal cubagemUnitaria = item.alturaCm().multiply(item.larguraCm()).multiply(item.comprimentoCm())
                    .divide(FATOR_CUBAGEM, 3, RoundingMode.HALF_UP);
            pesoCubado = pesoCubado.add(cubagemUnitaria.multiply(quantidade));
        }

        return pesoReal.max(pesoCubado);
    }

    /** Heurística simples por região (1º dígito do CEP) até termos a API real com prazos oficiais. */
    private int prazoPorRegiao(String cep) {
        int primeiroDigito = Character.getNumericValue(cep.charAt(0));
        return switch (primeiroDigito) {
            case 0, 1 -> 3; // SP
            case 2, 3 -> 4; // RJ/ES/MG
            case 4, 5 -> 5; // BA/SE/PE/AL/PB/RN/CE/PI/MA
            case 6 -> 6;    // Norte
            case 7 -> 4;    // DF/GO/TO/MT/MS
            default -> 5;   // Sul
        };
    }

    private record CorreiosPriceRequest(String cepDestino, BigDecimal pesoKg) {
    }

    private record CorreiosPriceResponse(BigDecimal valor, int prazoDias) {
    }
}
