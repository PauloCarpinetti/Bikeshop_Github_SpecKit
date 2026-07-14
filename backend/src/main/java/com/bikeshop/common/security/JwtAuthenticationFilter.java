package com.bikeshop.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());

      if (jwtService.isValid(token)) {
        Claims claims = jwtService.parseClaims(token);
        if ("access".equals(claims.get("type", String.class))) {
          String subject = claims.getSubject();
          @SuppressWarnings("unchecked")
          List<String> roles = claims.get("roles", List.class);
          List<GrantedAuthority> authorities =
              roles == null
                  ? List.of()
                  : roles.stream()
                      .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                      .map(GrantedAuthority.class::cast)
                      .toList();

          var authentication = new UsernamePasswordAuthenticationToken(subject, null, authorities);
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
