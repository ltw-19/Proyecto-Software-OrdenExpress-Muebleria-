package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;

/**
 * Lo que recibe el navegador al iniciar sesión: el token JWT y los datos mínimos
 * para armar la interfaz. Nunca incluye el hash de la contraseña.
 */
public record IngresoRespuesta(String token, String id, String nombre, String correo, Rol rol) {
}
