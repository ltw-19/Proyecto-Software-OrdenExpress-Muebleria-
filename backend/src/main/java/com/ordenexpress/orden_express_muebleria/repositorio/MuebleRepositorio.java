package com.ordenexpress.orden_express_muebleria.repositorio;

import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Acceso a muebles.json. (Las historias de Mueble le irán agregando métodos.) */
@Repository
public class MuebleRepositorio {

    private static final String ARCHIVO = "muebles.json";

    /** Forma de los códigos: M-001, M-002... */
    private static final Pattern PATRON_CODIGO = Pattern.compile("^M-(\\d{1,6})$");

    private final ServicioArchivoJson archivos;

    public MuebleRepositorio(ServicioArchivoJson archivos) {
        this.archivos = archivos;
    }

    public List<Mueble> listar() {
        return archivos.leerLista(ARCHIVO, Mueble.class);
    }

    public Optional<Mueble> buscarPorId(String id) {
        return listar().stream().filter(m -> id.equals(m.id())).findFirst();
    }

    public void guardarTodos(List<Mueble> nuevos) {
        archivos.modificarLista(ARCHIVO, Mueble.class, lista -> lista.addAll(nuevos));
    }

    /**
     * Leer - modificar - guardar muebles.json bajo un solo candado.
     * Lo usa Aprobar Pedido para descontar el stock de todos los ítems a la vez.
     * Si la operación lanza una excepción, el archivo NO cambia (todo o nada).
     * Las reglas (validar y restar unidades) las pone quien llama, no este repositorio.
     */
    public <R> R modificar(Function<List<Mueble>, R> operacion) {
        return archivos.modificarLista(ARCHIVO, Mueble.class, operacion);
    }

    /**
     * Agrega un mueble nuevo asignándole el siguiente código (M-001, M-002...).
     * Todo ocurre bajo un mismo candado: si dos personas registran a la vez, cada una
     * recibe un código distinto.
     *
     * @param fabrica recibe el código ya calculado y devuelve el mueble completo a guardar.
     * @return el mueble guardado.
     */
    public Mueble agregarConCodigoNuevo(Function<String, Mueble> fabrica) {
        return archivos.modificarLista(ARCHIVO, Mueble.class, lista -> {
            // El código nuevo es "el mayor número que exista + 1". Los muebles sin código
            // (guardados antes de existir este campo) se ignoran.
            int maximo = 0;
            for (Mueble existente : lista) {
                if (existente.codigo() == null) {
                    continue;
                }
                Matcher coincidencia = PATRON_CODIGO.matcher(existente.codigo());
                if (coincidencia.matches()) {
                    maximo = Math.max(maximo, Integer.parseInt(coincidencia.group(1)));
                }
            }
            Mueble nuevo = fabrica.apply(String.format(Locale.ROOT, "M-%03d", maximo + 1));
            lista.add(nuevo);
            return nuevo;
        });
    }
}
