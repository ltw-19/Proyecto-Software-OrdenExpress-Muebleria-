package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.CuentaRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.RegistroSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.modelo.Usuario;
import com.ordenexpress.orden_express_muebleria.repositorio.UsuarioRepositorio;
import com.ordenexpress.orden_express_muebleria.seguridad.ServicioJwt;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Lógica de la épica Gestionar Cuenta: iniciar sesión (pantalla de entrada) y Crear Cuenta. */
@Service
public class CuentaServicio {

    /** BCrypt solo usa los primeros 72 bytes de la clave; más allá se rechaza para no truncar en silencio. */
    private static final int MAXIMO_BYTES_BCRYPT = 72;

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;

    /** Hash de mentira para gastar el mismo tiempo cuando el correo no existe (ver ingresar). */
    private final String hashFicticio;

    public CuentaServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificador,
                          ServicioJwt servicioJwt) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
        this.hashFicticio = codificador.encode("clave-ficticia");
    }

    /**
     * Historia: CREAR CUENTA (CLIENTE).
     * Algoritmo:
     *  1. Rechaza claves que BCrypt no puede procesar completas (más de 72 bytes).
     *  2. Normaliza los datos: quita espacios sobrantes, el correo se guarda en minúsculas
     *     y una dirección vacía se guarda como "sin dirección".
     *  3. Arma el usuario con id UUID, hash BCrypt de la clave, rol CLIENTE y fecha de creación.
     *  4. Lo guarda solo si el correo no existe (comprobación y guardado atómicos).
     *  5. Si el correo ya existía responde 409; si no, devuelve la cuenta SIN la clave.
     */
    public CuentaRespuesta crearCuenta(RegistroSolicitud solicitud) {
        if (excedeLimiteBcrypt(solicitud.clave())) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga. Usa una más corta.");
        }

        String direccion = (solicitud.direccion() == null || solicitud.direccion().isBlank())
                ? null
                : solicitud.direccion().trim();

        Usuario nuevo = new Usuario(
                UUID.randomUUID().toString(),
                solicitud.nombre().trim(),
                solicitud.correo().trim().toLowerCase(Locale.ROOT),
                codificador.encode(solicitud.clave()),     // nunca se guarda la clave en texto plano
                solicitud.telefono().trim(),
                direccion,
                Rol.CLIENTE,                               // el rol lo fija el servidor, no el cliente
                Instant.now());

        if (!usuarioRepositorio.agregarSiCorreoNoExiste(nuevo)) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con ese correo.");
        }
        return aRespuesta(nuevo);
    }

    /**
     * Iniciar sesión (pantalla de entrada obligatoria).
     * Algoritmo:
     *  1. Busca al usuario por correo.
     *  2. Compara la clave escrita contra el hash BCrypt guardado.
     *  3. Si algo falla responde SIEMPRE el mismo mensaje (no revela si el correo existe).
     *  4. Si todo está bien, genera el JWT y devuelve los datos para armar la interfaz.
     */
    public IngresoRespuesta ingresar(IngresoSolicitud solicitud) {
        // Una clave de más de 72 bytes nunca pudo haberse registrado: se trata como incorrecta.
        if (excedeLimiteBcrypt(solicitud.clave())) {
            throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

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

    private static boolean excedeLimiteBcrypt(String clave) {
        return clave.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT;
    }

    private static CuentaRespuesta aRespuesta(Usuario usuario) {
        return new CuentaRespuesta(usuario.id(), usuario.nombre(), usuario.correo(),
                usuario.telefono(), usuario.direccion(), usuario.rol());
    }
}
