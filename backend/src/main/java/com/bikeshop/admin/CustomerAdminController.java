package com.bikeshop.admin;

import com.bikeshop.admin.dto.CustomerDto;
import com.bikeshop.admin.dto.UpdateCustomerStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta e bloqueio de clientes no backoffice (FR-009, T080). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/customers")
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

    public CustomerAdminController(CustomerAdminService customerAdminService) {
        this.customerAdminService = customerAdminService;
    }

    @GetMapping
    public List<CustomerDto> listar() {
        return customerAdminService.listar();
    }

    @PatchMapping("/{id}/status")
    public CustomerDto atualizarStatus(@PathVariable Long id, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return customerAdminService.atualizarStatus(id, request);
    }
}
