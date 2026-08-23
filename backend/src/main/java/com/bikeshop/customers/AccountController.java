package com.bikeshop.customers;

import com.bikeshop.customers.dto.EnderecoDto;
import com.bikeshop.customers.dto.EnderecoRequest;
import com.bikeshop.customers.dto.ProfileDto;
import com.bikeshop.customers.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil e endereços do cliente autenticado (FR-008, T055). Protegido por
 * {@code anyRequest().authenticated()} em {@code SecurityConfig} — não está na lista de rotas
 * públicas, então requer um JWT de acesso válido.
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final CustomerProfileService profileService;

    public AccountController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ProfileDto getProfile(Authentication authentication) {
        return profileService.getProfile(clienteId(authentication));
    }

    @PutMapping("/profile")
    public ProfileDto updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(clienteId(authentication), request);
    }

    @GetMapping("/addresses")
    public List<EnderecoDto> listAddresses(Authentication authentication) {
        return profileService.listAddresses(clienteId(authentication));
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoDto createAddress(Authentication authentication, @Valid @RequestBody EnderecoRequest request) {
        return profileService.createAddress(clienteId(authentication), request);
    }

    @PutMapping("/addresses/{id}")
    public EnderecoDto updateAddress(Authentication authentication, @PathVariable Long id,
                                      @Valid @RequestBody EnderecoRequest request) {
        return profileService.updateAddress(clienteId(authentication), id, request);
    }

    private Long clienteId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
