package com.bikeshop.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

  public NotFoundException(String entity, Object id) {
    super("NOT_FOUND", "%s não encontrado(a): %s".formatted(entity, id), HttpStatus.NOT_FOUND);
  }
}
