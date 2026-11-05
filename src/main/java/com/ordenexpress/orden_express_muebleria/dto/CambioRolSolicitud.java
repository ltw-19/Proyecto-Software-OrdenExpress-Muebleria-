package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/cuentas/{id}/rol (solo ADMIN). Valores válidos: ADMIN, GESTOR, CLIENTE. */
public record CambioRolSolicitud(@NotNull(message = "El rol es obligatorio.") Rol rol) {
}
