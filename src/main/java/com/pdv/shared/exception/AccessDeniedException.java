package com.pdv.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends PdvException {
    public AccessDeniedException(String message) { super(message); }
    public AccessDeniedException() { super("Acesso negado: permissão insuficiente para esta operação"); }
}
