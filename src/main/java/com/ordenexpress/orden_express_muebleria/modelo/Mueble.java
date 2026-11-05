package com.ordenexpress.orden_express_muebleria.modelo;

import java.math.BigDecimal;

/**
 * Entidad guardada en muebles.json.
 * Solo se guarda el precio base en dólares (precioUsd); los bolívares se calculan
 * al responder, usando la tasa BCV configurada.
 *
 * id      : identificador interno (UUID).
 * codigo  : código legible para las personas (M-001, M-002...), lo genera el servidor.
 * unidades: stock disponible. Puede ser null en muebles guardados antes de existir este campo.
 */
public record Mueble(
        String id,
        String codigo,
        String nombre,
        String categoria,
        String descripcion,
        Dimensiones dimensiones,
        String material,
        String urlFoto,
        BigDecimal precioUsd,
        Integer unidades) {

    /** Dimensiones en centímetros. */
    public record Dimensiones(BigDecimal ancho, BigDecimal alto, BigDecimal profundidad) {
    }
}
