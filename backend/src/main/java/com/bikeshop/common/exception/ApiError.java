package com.bikeshop.common.exception;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro da API (usado por todos os módulos). */
public record ApiError(String code, String message, List<String> details, Instant timestamp) {
  public static ApiError of(String code, String message) {
    return new ApiError(code, message, List.of(), Instant.now());
  }

  public static ApiError of(String code, String message, List<String> details) {
    return new ApiError(code, message, details, Instant.now());
  }
}
