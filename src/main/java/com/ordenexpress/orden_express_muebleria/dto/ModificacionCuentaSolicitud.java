package com.ordenexpress.orden_express_muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PUT /api/cuentas/yo (historia "Modificar Cuenta").
 * Reemplaza los datos personales de la cuenta que hace la petición.
 *
 * No tiene "rol" ni "clave": el rol solo lo cambia el ADMIN con otro endpoint, y
 * la contraseña se cambiará en la historia "Cambiar Contraseña" (Sprint 2).
 */
public record ModificacionCuentaSolicitud(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.") String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido admite máximo 100 caracteres.") String apellido,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo no tiene un formato válido.")
        String correo,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$",
                message = "El teléfono debe tener entre 10 y 15 dígitos (puede iniciar con +).")
        String telefono,

        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion) {
}
