package com.ordenexpress.orden_express_muebleria.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carrito tal como lo ve el navegador. Todos los montos los calcula el backend
 * (precios de muebles.json, tasa BCV, BigDecimal, 2 decimales, HALF_UP).
 *
 *  - totalUsd: suma de los subtotales en USD.
 *  - totalBs : se convierte el TOTAL en USD (no se suman bolívares ya redondeados).
 */
public record CarritoRespuesta(
        List<ItemCarritoRespuesta> items,
        int cantidadItems,
        BigDecimal totalUsd,
        BigDecimal totalBs) {

    /**
     * Una línea del carrito con los datos del mueble necesarios para mostrarla.
     * disponible: true si al mueble le quedan unidades (el stock pudo cambiar desde que se añadió).
     *             Como en el catálogo, el CLIENTE nunca recibe la cifra exacta.
     */
    public record ItemCarritoRespuesta(
            String idMueble,
            String codigo,
            String nombre,
            String urlFoto,
            BigDecimal precioUsd,
            BigDecimal precioBs,
            int cantidad,
            BigDecimal subtotalUsd,
            BigDecimal subtotalBs,
            boolean disponible) {
    }
}
