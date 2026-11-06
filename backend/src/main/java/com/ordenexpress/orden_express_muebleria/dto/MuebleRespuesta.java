package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Mueble;

import java.math.BigDecimal;

/**
 * Mueble tal como lo ve el navegador. Incluye el precio bimonetario: precioUsd viene del
 * archivo y precioBs se calcula al responder con la tasa BCV.
 * "unidades" (stock) es información para ADMIN y GESTOR.
 */
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
        Integer unidades) {
}
