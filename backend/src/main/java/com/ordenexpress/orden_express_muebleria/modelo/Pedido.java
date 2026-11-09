package com.ordenexpress.orden_express_muebleria.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Entidad guardada en pedidos.json.
 *
 * id         : identificador interno (UUID).
 * numero     : número legible para las personas (OE-0001, OE-0002...), lo genera el servidor.
 * idUsuario  : cliente que registró el pedido (sale SIEMPRE del token).
 * items      : copia de los muebles del carrito con su precio CONGELADO al registrar.
 * tasaBcv    : tasa (Bs. por 1 USD) congelada al registrar. Si la tasa cambia después,
 *              este pedido conserva sus montos.
 * totalUsd   : suma de los subtotales en USD.
 * totalBs    : totalUsd × tasaBcv (se convierte el total, no se suman Bs. redondeados).
 * entrega    : datos de entrega (nombre, apellido, teléfono, dirección).
 * estado     : todo pedido nuevo nace PENDIENTE.
 * timestamp  : momento del registro. Es inmutable: ninguna historia lo modifica.
 */
public record Pedido(
        String id,
        String numero,
        String idUsuario,
        List<ItemPedido> items,
        BigDecimal tasaBcv,
        BigDecimal totalUsd,
        BigDecimal totalBs,
        DatosEntrega entrega,
        EstadoPedido estado,
        Instant timestamp) {

    /**
     * Un mueble del pedido. Se copian código y nombre para que el pedido se pueda leer
     * aunque el mueble cambie en el catálogo más adelante.
     */
    public record ItemPedido(
            String idMueble,
            String codigo,
            String nombre,
            int cantidad,
            BigDecimal precioUnitarioUsd,
            BigDecimal precioUnitarioBs,
            BigDecimal subtotalUsd,
            BigDecimal subtotalBs) {
    }

    /** Datos de entrega del pedido (pueden ser distintos a los de la cuenta). */
    public record DatosEntrega(
            String nombre,
            String apellido,
            String telefono,
            String direccion) {
    }
}
