package com.pdv.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends PdvException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " não encontrado(a) com id: " + id);
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
