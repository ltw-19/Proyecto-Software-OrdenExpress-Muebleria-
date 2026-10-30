package com.ordenexpress.orden_express_muebleria.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Error controlado de negocio o seguridad (por ejemplo "clave incorrecta" o
 * "no puedes eliminar este pedido"). Lleva el código HTTP que debe recibir el cliente.
 */
public class ExcepcionNegocio extends RuntimeException {

    private final HttpStatus estado;

    public ExcepcionNegocio(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
