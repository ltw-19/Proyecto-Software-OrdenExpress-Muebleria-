package com.ordenexpress.orden_express_muebleria.repositorio;

import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a muebles.json. (Las historias de Mueble le irán agregando métodos.) */
@Repository
public class MuebleRepositorio {

    private static final String ARCHIVO = "muebles.json";

    private final ServicioArchivoJson archivos;

    public MuebleRepositorio(ServicioArchivoJson archivos) {
        this.archivos = archivos;
    }

    public List<Mueble> listar() {
        return archivos.leerLista(ARCHIVO, Mueble.class);
    }

    public void guardarTodos(List<Mueble> nuevos) {
        archivos.modificarLista(ARCHIVO, Mueble.class, lista -> lista.addAll(nuevos));
    }
}
