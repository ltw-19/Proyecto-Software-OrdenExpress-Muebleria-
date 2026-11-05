package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.CuentaRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.ModificacionCuentaSolicitud;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Lógica de la épica Gestionar Cuenta: iniciar sesión (pantalla de entrada), Crear Cuenta
 * y Modificar Cuenta (datos personales y, solo para el ADMIN, roles).
 */
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

    // ------------------------------------------------------------------
    // Crear Cuenta
    // ------------------------------------------------------------------

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

        Usuario nuevo = new Usuario(
                UUID.randomUUID().toString(),
                solicitud.nombre().trim(),
                solicitud.apellido().trim(),
                normalizarCorreo(solicitud.correo()),
                codificador.encode(solicitud.clave()),     // nunca se guarda la clave en texto plano
                solicitud.telefono().trim(),
                limpiarDireccion(solicitud.direccion()),
                Rol.CLIENTE,                               // el rol lo fija el servidor, no el cliente
                Instant.now());

        if (!usuarioRepositorio.agregarSiCorreoNoExiste(nuevo)) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con ese correo.");
        }
        return aRespuesta(nuevo);
    }

    // ------------------------------------------------------------------
    // Modificar Cuenta
    // ------------------------------------------------------------------

    /**
     * Historia: MODIFICAR CUENTA, parte del titular (ADMIN y CLIENTE editan SUS datos).
     * Algoritmo:
     *  1. El id de la cuenta sale del token (nunca de lo que mande el navegador): nadie puede
     *     editar la cuenta de otra persona con este método.
     *  2. Normaliza los datos igual que al crear la cuenta.
     *  3. El repositorio, bajo un candado, comprueba que el correo no pertenezca a OTRA cuenta
     *     y reemplaza solo los datos personales (el rol, la clave y la fecha no se tocan).
     *  4. Devuelve la cuenta actualizada, o 409 si el correo está en uso.
     */
    public CuentaRespuesta modificarMisDatos(String idUsuario, ModificacionCuentaSolicitud solicitud) {
        var resultado = usuarioRepositorio.actualizarDatosPersonales(
                idUsuario,
                solicitud.nombre().trim(),
                solicitud.apellido().trim(),
                normalizarCorreo(solicitud.correo()),
                solicitud.telefono().trim(),
                limpiarDireccion(solicitud.direccion()));

        return switch (resultado.estado()) {
            case ACTUALIZADO -> aRespuesta(resultado.usuario());
            case CORREO_EN_USO -> throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                    "Ya existe una cuenta registrada con ese correo.");
            case NO_ENCONTRADO -> throw new ExcepcionNegocio(HttpStatus.NOT_FOUND, "La cuenta no existe.");
        };
    }

    /**
     * Modificar Cuenta, parte del ADMIN: lista todas las cuentas para poder elegir a quién
     * cambiarle el rol. Ordenadas por correo; nunca incluye claves.
     */
    public List<CuentaRespuesta> listarCuentas() {
        return usuarioRepositorio.listar().stream()
                .sorted(Comparator.comparing(Usuario::correo, String.CASE_INSENSITIVE_ORDER))
                .map(CuentaServicio::aRespuesta)
                .toList();
    }

    /**
     * Modificar Cuenta, parte del ADMIN: cambia el rol de una cuenta (por ejemplo, CLIENTE a GESTOR).
     * Reglas:
     *  - Un ADMIN no puede cambiar su PROPIO rol: así el sistema nunca se queda sin administrador.
     *  - Si la cuenta no existe, 404.
     * El cambio rige de inmediato: el filtro JWT relee el rol del archivo en cada petición.
     */
    public CuentaRespuesta cambiarRol(String idAdminActual, String idObjetivo, Rol nuevoRol) {
        if (idAdminActual.equals(idObjetivo)) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol.");
        }
        return usuarioRepositorio.actualizarRol(idObjetivo, nuevoRol)
                .map(CuentaServicio::aRespuesta)
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "La cuenta no existe."));
    }

    // ------------------------------------------------------------------
    // Iniciar sesión
    // ------------------------------------------------------------------

    /**
     * Iniciar sesión (pantalla de entrada obligatoria).
     * Algoritmo:
     *  1. Busca al usuario por correo.
     *  2. Compara la clave escrita contra el hash BCrypt guardado.
     *  3. Si algo falla responde SIEMPRE el mismo mensaje (no revela si el correo existe).
     *  4. Si todo está bien, genera el JWT y devuelve los datos de la cuenta para armar la interfaz.
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
        return new IngresoRespuesta(servicioJwt.generarToken(usuario), usuario.id(), usuario.nombre(),
                usuario.apellido(), usuario.correo(), usuario.telefono(), usuario.direccion(), usuario.rol());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static boolean excedeLimiteBcrypt(String clave) {
        return clave.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT;
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    /** Una dirección vacía se guarda como "sin dirección" (null), no como texto en blanco. */
    private static String limpiarDireccion(String direccion) {
        return (direccion == null || direccion.isBlank()) ? null : direccion.trim();
    }

    private static CuentaRespuesta aRespuesta(Usuario usuario) {
        return new CuentaRespuesta(usuario.id(), usuario.nombre(), usuario.apellido(), usuario.correo(),
                usuario.telefono(), usuario.direccion(), usuario.rol());
    }
}
