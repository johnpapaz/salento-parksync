package com.parksync.command;

/**
 * Excepción lanzada cuando el backend está bajo carga extrema.
 * El controlador la convierte en HTTP 503 + Retry-After (RTF-10).
 */
public class ServiceOverloadedException extends RuntimeException {

    private final int retryAfterSeconds;

    public ServiceOverloadedException(int retryAfterSeconds) {
        super("Servicio temporalmente sobrecargado. Reintente en " + retryAfterSeconds + " segundos.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
