package com.bikeshop.admin;

import com.bikeshop.admin.dto.CouponDto;
import com.bikeshop.admin.dto.CreateCouponRequest;
import com.bikeshop.admin.dto.UpdateCouponRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de cupons de desconto no backoffice (FR-009, T079). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
public class CouponAdminController {

    private final CouponAdminService couponAdminService;

    public CouponAdminController(CouponAdminService couponAdminService) {
        this.couponAdminService = couponAdminService;
    }

    @GetMapping
    public List<CouponDto> listar() {
        return couponAdminService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponDto criar(@Valid @RequestBody CreateCouponRequest request) {
        return couponAdminService.criar(request);
    }

    @PutMapping("/{id}")
    public CouponDto atualizar(@PathVariable Long id, @Valid @RequestBody UpdateCouponRequest request) {
        return couponAdminService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        couponAdminService.desativar(id);
    }
}
