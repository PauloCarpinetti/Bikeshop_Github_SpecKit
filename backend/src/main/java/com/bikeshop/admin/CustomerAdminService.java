package com.bikeshop.admin;

import com.bikeshop.admin.dto.CustomerDto;
import com.bikeshop.admin.dto.UpdateCustomerStatusRequest;
import com.bikeshop.audit.AuditService;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.common.security.Role;
import com.bikeshop.customers.Cliente;
import com.bikeshop.customers.ClienteRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e bloqueio/desbloqueio de clientes no backoffice (FR-009, T080). Não edita dados
 * pessoais do cliente (gestão exclusiva do próprio cliente, Princípio II) e não permite bloquear
 * contas administrativas/operacionais.
 */
@Service
@Transactional
public class CustomerAdminService {

    private final ClienteRepository clienteRepository;
    private final AuditService auditService;

    public CustomerAdminService(ClienteRepository clienteRepository, AuditService auditService) {
        this.clienteRepository = clienteRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> listar() {
        return clienteRepository.findByRoleOrderByCriadoEmDesc(Role.CUSTOMER).stream().map(this::toDto).toList();
    }

    public CustomerDto atualizarStatus(Long id, UpdateCustomerStatusRequest request) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new NotFoundException("Cliente", id));
        if (cliente.getRole() != Role.CUSTOMER) {
            throw new BusinessException("CLIENTE_INVALIDO",
                    "Somente contas de cliente podem ser bloqueadas/desbloqueadas", HttpStatus.BAD_REQUEST);
        }

        boolean estadoAnterior = cliente.isBloqueado();
        if (request.bloqueado()) {
            cliente.bloquear();
        } else {
            cliente.desbloquear();
        }

        auditService.record("ATUALIZAR_STATUS_CLIENTE", "Cliente", String.valueOf(id),
                Map.of("bloqueado", estadoAnterior), Map.of("bloqueado", cliente.isBloqueado()));

        return toDto(cliente);
    }

    private CustomerDto toDto(Cliente cliente) {
        return new CustomerDto(cliente.getId(), cliente.getNome(), cliente.getEmail(), cliente.getTelefone(),
                cliente.isBloqueado(), cliente.getCriadoEm());
    }
}
