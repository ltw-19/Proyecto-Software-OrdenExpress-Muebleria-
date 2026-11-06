package com.ordenexpress.orden_express_muebleria.repositorio;

import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.modelo.Usuario;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Acceso a usuarios.json. Solo sabe leer y guardar usuarios; no contiene reglas de negocio. */
@Repository
public class UsuarioRepositorio {

    private static final String ARCHIVO = "usuarios.json";

    private final ServicioArchivoJson archivos;

    public UsuarioRepositorio(ServicioArchivoJson archivos) {
        this.archivos = archivos;
    }

    /** Resultado de actualizar los datos personales de una cuenta. */
    public record ResultadoActualizacion(Estado estado, Usuario usuario) {
        public enum Estado { ACTUALIZADO, NO_ENCONTRADO, CORREO_EN_USO }
    }

    public List<Usuario> listar() {
        return archivos.leerLista(ARCHIVO, Usuario.class);
    }

    public Optional<Usuario> buscarPorId(String id) {
        return listar().stream().filter(u -> u.id().equals(id)).findFirst();
    }

    /** El correo se compara sin importar mayúsculas ni espacios sobrantes. */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        String buscado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return listar().stream().filter(u -> u.correo().equalsIgnoreCase(buscado)).findFirst();
    }

    /**
     * Agrega un usuario SOLO si su correo todavía no existe. La comprobación y el guardado
     * ocurren bajo el mismo candado: si dos personas se registran a la vez con el mismo correo,
     * solo una lo logra.
     *
     * @return true si se agregó; false si el correo ya estaba registrado (el archivo no cambia).
     */
    public boolean agregarSiCorreoNoExiste(Usuario nuevo) {
        return archivos.modificarLista(ARCHIVO, Usuario.class, lista -> {
            boolean existe = lista.stream().anyMatch(u -> u.correo().equalsIgnoreCase(nuevo.correo()));
            if (existe) {
                return false;
            }
            lista.add(nuevo);
            return true;
        });
    }

    /** Agrega varios usuarios de una sola vez (lectura + escritura bajo el mismo candado). */
    public void guardarTodos(List<Usuario> nuevos) {
        archivos.modificarLista(ARCHIVO, Usuario.class, lista -> lista.addAll(nuevos));
    }

    /**
     * Cambia los datos personales de una cuenta. Todo ocurre bajo un mismo candado y sobre el
     * registro más reciente del archivo: así NO se pisan cambios hechos al mismo tiempo (por
     * ejemplo, un cambio de rol del ADMIN) porque el rol, la clave y la fecha de creación se
     * toman del registro actual, no de una copia vieja.
     */
    public ResultadoActualizacion actualizarDatosPersonales(String id, String nombre, String apellido,
                                                            String correo, String telefono, String direccion) {
        return archivos.modificarLista(ARCHIVO, Usuario.class, lista -> {
            int posicion = posicionDe(lista, id);
            if (posicion < 0) {
                return new ResultadoActualizacion(ResultadoActualizacion.Estado.NO_ENCONTRADO, null);
            }
            // El correo debe ser único entre las DEMÁS cuentas (la propia cuenta puede conservarlo).
            boolean correoEnUso = lista.stream()
                    .anyMatch(u -> !u.id().equals(id) && u.correo().equalsIgnoreCase(correo));
            if (correoEnUso) {
                return new ResultadoActualizacion(ResultadoActualizacion.Estado.CORREO_EN_USO, null);
            }
            Usuario actual = lista.get(posicion);
            Usuario actualizado = new Usuario(actual.id(), nombre, apellido, correo, actual.claveHash(),
                    telefono, direccion, actual.rol(), actual.fechaCreacion());
            lista.set(posicion, actualizado);
            return new ResultadoActualizacion(ResultadoActualizacion.Estado.ACTUALIZADO, actualizado);
        });
    }

    /** Cambia solo el rol de una cuenta. Devuelve vacío si la cuenta no existe. */
    public Optional<Usuario> actualizarRol(String id, Rol nuevoRol) {
        return archivos.modificarLista(ARCHIVO, Usuario.class, lista -> {
            int posicion = posicionDe(lista, id);
            if (posicion < 0) {
                return Optional.<Usuario>empty();
            }
            Usuario actual = lista.get(posicion);
            Usuario actualizado = new Usuario(actual.id(), actual.nombre(), actual.apellido(), actual.correo(),
                    actual.claveHash(), actual.telefono(), actual.direccion(), nuevoRol, actual.fechaCreacion());
            lista.set(posicion, actualizado);
            return Optional.of(actualizado);
        });
    }

    private static int posicionDe(List<Usuario> lista, String id) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
