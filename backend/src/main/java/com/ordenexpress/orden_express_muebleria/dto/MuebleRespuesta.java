package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Mueble;

import java.math.BigDecimal;

/**
 * Mueble tal como lo ve el navegador. Incluye el precio bimonetario: precioUsd viene del
 * archivo y precioBs se calcula al responder con la tasa BCV.
 * "disponible" indica si el producto tiene stock > 0.
 * "unidades" (stock numérico) es información exclusiva para ADMIN y GESTOR (para CLIENTE es null).
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
        Boolean disponible,
        Integer unidades) {
}
