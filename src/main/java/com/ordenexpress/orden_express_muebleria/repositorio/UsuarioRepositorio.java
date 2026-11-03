package com.ordenexpress.orden_express_muebleria.repositorio;

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
}
