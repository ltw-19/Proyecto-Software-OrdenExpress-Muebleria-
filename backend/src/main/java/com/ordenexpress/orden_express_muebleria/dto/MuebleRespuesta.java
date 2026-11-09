package com.ordenexpress.orden_express_muebleria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;

import java.math.BigDecimal;

/**
 * Mueble tal como lo ve el navegador.
 *
 *  - precioUsd viene del archivo; precioBs se calcula al responder con la tasa BCV.
 *  - disponible : true si quedan unidades. Lo reciben todos los roles.
 *  - unidades   : la cifra exacta del stock. SOLO la reciben ADMIN y GESTOR; para el CLIENTE
 *                 llega como null y, gracias a @JsonInclude, ni siquiera aparece en el JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MuebleRespuesta(
        String id,
        String codigo,
        String nombre,
        String categoria,
        String descripcion,
        Mueble.Dimensiones dimensiones,
        String material,
        String urlFoto,
        BigDecimal precioUsd,
        BigDecimal precioBs,
        boolean disponible,
        Integer unidades) {
}
