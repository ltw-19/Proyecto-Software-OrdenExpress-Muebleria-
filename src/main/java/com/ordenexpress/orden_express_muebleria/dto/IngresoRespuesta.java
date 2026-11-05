package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;

/**
 * Lo que recibe el navegador al iniciar sesión: el token JWT y los datos de la cuenta,
 * suficientes para armar la interfaz y precargar el formulario de "Datos personales"
 * sin necesitar un endpoint de consulta (Consultar Cuenta es del Sprint 2).
 * Nunca incluye el hash de la contraseña.
 */
public record IngresoRespuesta(String token, String id, String nombre, String apellido, String correo,
                               String telefono, String direccion, Rol rol) {
}
