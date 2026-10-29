package com.pdv.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ===== BASE =====
public class PdvException extends RuntimeException {
    public PdvException(String message) { super(message); }
    public PdvException(String message, Throwable cause) { super(message, cause); }
}
