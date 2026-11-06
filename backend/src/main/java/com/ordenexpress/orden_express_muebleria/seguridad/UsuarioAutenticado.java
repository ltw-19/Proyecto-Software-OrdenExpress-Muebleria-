package com.ordenexpress.orden_express_muebleria.seguridad;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;

/**
 * Datos del usuario que hizo la petición actual (ya validado por el filtro JWT).
 * A propósito NO incluye el hash de la contraseña. Los controladores y servicios
 * lo reciben con @AuthenticationPrincipal.
 */
public record UsuarioAutenticado(String id, String correo, Rol rol) {
}
