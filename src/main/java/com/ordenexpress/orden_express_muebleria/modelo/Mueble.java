package com.ordenexpress.orden_express_muebleria.modelo;

import java.math.BigDecimal;

/**
 * Entidad guardada en muebles.json.
 * Solo se guarda el precio base en dólares (precioUsd); los bolívares se calculan
 * al responder, usando la tasa BCV configurada.
 */
public record Mueble(
        String id,
        String nombre,
        String categoria,
        Dimensiones dimensiones,
        String material,
        String urlFoto,
        BigDecimal precioUsd) {

    /** Dimensiones en centímetros. */
    public record Dimensiones(BigDecimal ancho, BigDecimal alto, BigDecimal profundidad) {
    }
}
