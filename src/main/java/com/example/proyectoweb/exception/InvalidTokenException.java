package com.example.proyectoweb.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token inválido o expirado");
    }

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
