package com.ordenexpress.orden_express_muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/cuentas/ingresar. */
public record IngresoSolicitud(
        @NotBlank(message = "El correo es obligatorio.") String correo,

        // BCrypt solo procesa los primeros 72 bytes; se limita para evitar claves truncadas.
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 64, message = "La contraseña admite máximo 64 caracteres.") String clave) {
}
