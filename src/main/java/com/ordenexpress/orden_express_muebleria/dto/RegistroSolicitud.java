package com.ordenexpress.orden_express_muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/cuentas/registro (historia "Crear Cuenta").
 *
 * A propósito NO tiene campo "rol": el rol no lo decide quien se registra.
 * Toda cuenta nueva es CLIENTE; para ascenderla existe "Modificar Cuenta" (solo ADMIN).
 */
public record RegistroSolicitud(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.") String nombre,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo no tiene un formato válido.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.") String clave,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$",
                message = "El teléfono debe tener entre 10 y 15 dígitos (puede iniciar con +).")
        String telefono,

        // Opcional: se puede completar después en "Modificar Cuenta" o al registrar un pedido.
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion) {
}
