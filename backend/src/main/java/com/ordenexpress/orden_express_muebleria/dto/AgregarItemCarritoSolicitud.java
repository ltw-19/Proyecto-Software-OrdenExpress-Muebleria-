package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.util.DeserializadorCantidadEntera;
import tools.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/carrito/items (historia "Añadir ítem al Carrito").
 *
 * A propósito NO incluye precio ni id de usuario: el precio sale de muebles.json y
 * el dueño del carrito sale del token.
 */
public record AgregarItemCarritoSolicitud(
        @NotBlank(message = "El mueble es obligatorio.")
        @Size(max = 100, message = "El identificador del mueble admite máximo 100 caracteres.")
        String idMueble,

        // Formato: solo enteros escritos como número (1.5, "2" o 1e2 → 400).
        // Valor: obligatorio y positivo (null, 0 o -1 → 400), con un máximo razonable.
        @JsonDeserialize(using = DeserializadorCantidadEntera.class)
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        @Max(value = 100000, message = "La cantidad admite máximo 100000.")
        Integer cantidad) {
}
