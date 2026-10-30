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

    /** Agrega varios usuarios de una sola vez (lectura + escritura bajo el mismo candado). */
    public void guardarTodos(List<Usuario> nuevos) {
        archivos.modificarLista(ARCHIVO, Usuario.class, lista -> lista.addAll(nuevos));
    }
}
