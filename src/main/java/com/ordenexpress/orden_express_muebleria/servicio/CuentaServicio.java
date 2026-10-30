package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Usuario;
import com.ordenexpress.orden_express_muebleria.repositorio.UsuarioRepositorio;
import com.ordenexpress.orden_express_muebleria.seguridad.ServicioJwt;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Lógica de la épica Gestionar Cuenta. Por ahora: iniciar sesión (pantalla de entrada obligatoria). */
@Service
public class CuentaServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;

    /** Hash de prueba para gastar el mismo tiempo cuando el correo no existe (ver ingresar). */
    private final String hashFicticio;

    public CuentaServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificador,
                          ServicioJwt servicioJwt) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
        this.hashFicticio = codificador.encode("clave-ficticia");
    }

    /**
     * Algoritmo:
     *  1. Busca al usuario por correo.
     *  2. Compara la clave escrita contra el hash BCrypt guardado.
     *  3. Si algo falla responde SIEMPRE el mismo mensaje (no revela si el correo existe).
     *  4. Si todo está bien, genera el JWT y devuelve los datos para armar la interfaz.
     */
    public IngresoRespuesta ingresar(IngresoSolicitud solicitud) {
        Optional<Usuario> encontrado = usuarioRepositorio.buscarPorCorreo(solicitud.correo());

        // Se compara siempre (con hash ficticio si no existe) para que responder
        // tarde lo mismo exista o no el correo.
        String hash = encontrado.map(Usuario::claveHash).orElse(hashFicticio);
        boolean claveCorrecta = codificador.matches(solicitud.clave(), hash);

        if (encontrado.isEmpty() || !claveCorrecta) {
            throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        Usuario usuario = encontrado.get();
        return new IngresoRespuesta(servicioJwt.generarToken(usuario), usuario.id(),
                usuario.nombre(), usuario.correo(), usuario.rol());
    }
}
