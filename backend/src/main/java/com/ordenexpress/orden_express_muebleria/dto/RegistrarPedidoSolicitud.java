package com.ordenexpress.orden_express_muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/pedidos (historia "Registrar Pedido").
 *
 * A propósito SOLO trae los datos de entrega: los muebles y cantidades salen del carrito
 * guardado en carritos.json, los precios de muebles.json y el cliente del token.
 * El navegador nunca decide qué se compra ni cuánto cuesta.
 *
 * La dirección es obligatoria aquí (en la cuenta es opcional) porque sin ella no hay entrega.
 */
public record RegistrarPedidoSolicitud(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.") String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido admite máximo 100 caracteres.") String apellido,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$",
                message = "El teléfono debe tener entre 10 y 15 dígitos (puede iniciar con +).")
        String telefono,

        @NotBlank(message = "La dirección de entrega es obligatoria.")
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion) {
}
