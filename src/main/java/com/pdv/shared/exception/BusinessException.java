package com.pdv.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessException extends PdvException {
    public BusinessException(String message) { super(message); }
}
