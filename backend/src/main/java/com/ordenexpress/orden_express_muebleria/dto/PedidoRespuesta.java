package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.EstadoPedido;
import com.ordenexpress.orden_express_muebleria.modelo.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Pedido tal como lo ve el navegador. Todos los montos son los CONGELADOS al registrar
 * (no se recalculan con precios ni tasa actuales).
 *
 * enlaceWhatsapp: enlace wa.me al número del Gestor de Ventas con el mensaje
 *                 pre-estructurado del pedido. No se guarda: se arma al responder.
 */
public record PedidoRespuesta(
        String id,
        String numero,
        EstadoPedido estado,
        Instant timestamp,
        List<Pedido.ItemPedido> items,
        BigDecimal tasaBcv,
        BigDecimal totalUsd,
        BigDecimal totalBs,
        Pedido.DatosEntrega entrega,
        String enlaceWhatsapp) {
}
