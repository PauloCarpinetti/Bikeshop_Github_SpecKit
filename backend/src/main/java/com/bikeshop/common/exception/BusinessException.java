package com.bikeshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para violações de regra de negócio (ex.: estoque insuficiente, cupom expirado,
 * transição de status inválida). Cada módulo deve estender esta classe com um código estável em vez
 * de lançar exceções genéricas.
 */
public class BusinessException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  public BusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
