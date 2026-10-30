package com.ordenexpress.orden_express_muebleria.modelo;

import java.time.Instant;

/**
 * Entidad guardada en usuarios.json.
 * La contraseña NUNCA se guarda en texto plano: solo el hash BCrypt (claveHash).
 * telefono y direccion pueden ser null (los usuarios semilla no los tienen) y,
 * si lo son, simplemente no se escriben en el JSON.
 */
public record Usuario(
        String id,
        String nombre,
        String correo,
        String claveHash,
        String telefono,
        String direccion,
        Rol rol,
        Instant fechaCreacion) {
}
