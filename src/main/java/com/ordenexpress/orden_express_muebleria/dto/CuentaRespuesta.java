package com.ordenexpress.orden_express_muebleria.dto;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;

/**
 * Datos de una cuenta tal como los ve el navegador.
 * Nunca incluye claveHash.
 */
public record CuentaRespuesta(String id, String nombre, String apellido, String correo,
                              String telefono, String direccion, Rol rol) {
}
