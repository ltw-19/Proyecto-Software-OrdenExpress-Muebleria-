package com.ordenexpress.orden_express_muebleria.modelo;

import java.time.Instant;
import java.util.List;

/**
 * Entidad guardada en carritos.json. Hay un carrito por cliente.
 *
 * idUsuario         : dueño del carrito (sale SIEMPRE del token, nunca del navegador).
 * items             : muebles elegidos. Nunca es null (como mínimo, lista vacía).
 * fechaActualizacion: último cambio del carrito.
 *
 * A propósito NO se guardan precios ni totales: se calculan al responder con los precios
 * actuales de muebles.json y la tasa BCV. Así el navegador nunca decide un monto y el carrito
 * no queda con cifras viejas. El precio se "congela" recién al registrar el pedido.
 */
public record Carrito(
        String idUsuario,
        List<ItemCarrito> items,
        Instant fechaActualizacion) {

    /** Un mueble dentro del carrito con la cantidad elegida al añadirlo. */
    public record ItemCarrito(
            String idMueble,
            Integer cantidad,
            Instant fechaAgregado) {
    }
}
