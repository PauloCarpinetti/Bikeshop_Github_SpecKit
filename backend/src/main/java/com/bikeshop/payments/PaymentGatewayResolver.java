package com.bikeshop.payments;

import com.bikeshop.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolve o adapter correto (Strategy) a partir do {@link PaymentProvider} escolhido no checkout ou
 * informado na rota do webhook.
 */
@Component
public class PaymentGatewayResolver {

    private final Map<PaymentProvider, PaymentGatewayAdapter> adapters;

    public PaymentGatewayResolver(List<PaymentGatewayAdapter> adapters) {
        this.adapters = adapters.stream().collect(Collectors.toMap(PaymentGatewayAdapter::getProvider, Function.identity()));
    }

    public PaymentGatewayAdapter resolve(PaymentProvider provider) {
        PaymentGatewayAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new BusinessException("GATEWAY_NAO_SUPORTADO", "Gateway de pagamento não suportado: " + provider, HttpStatus.BAD_REQUEST);
        }
        return adapter;
    }

    public PaymentGatewayAdapter resolveByPathSegment(String provider) {
        try {
            return resolve(PaymentProvider.valueOf(toEnumName(provider)));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("GATEWAY_NAO_SUPORTADO", "Gateway de pagamento não suportado: " + provider, HttpStatus.BAD_REQUEST);
        }
    }

    private String toEnumName(String provider) {
        return provider.trim().toUpperCase().replace('-', '_');
    }
}
