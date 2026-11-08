package com.ordenexpress.orden_express_muebleria.repositorio;

import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Acceso a carritos.json. Solo sabe leer y guardar carritos; las reglas de negocio están en CarritoServicio. */
@Repository
public class CarritoRepositorio {

    private static final String ARCHIVO = "carritos.json";

    private final ServicioArchivoJson archivos;

    public CarritoRepositorio(ServicioArchivoJson archivos) {
        this.archivos = archivos;
    }

    /** Resultado de intentar añadir un ítem. */
    public record ResultadoAgregar(Estado estado, Carrito carrito) {
        public enum Estado { AGREGADO, YA_EXISTE }
    }

    /**
     * Devuelve el carrito del usuario, o vacío si todavía no tiene uno.
     * Solo LEE el archivo: consultar nunca crea ni modifica carritos.
     */
    public Optional<Carrito> buscarPorUsuario(String idUsuario) {
        return archivos.leerLista(ARCHIVO, Carrito.class).stream()
                .filter(c -> idUsuario.equals(c.idUsuario()))
                .findFirst();
    }

    /**
     * Añade un ítem al carrito del usuario SOLO si ese mueble todavía no está en él.
     * Si el usuario aún no tiene carrito, se le crea uno.
     *
     * La comprobación y el guardado ocurren bajo el mismo candado: si el cliente hace doble clic
     * en "Añadir", solo la primera petición guarda el ítem y la segunda recibe YA_EXISTE.
     *
     * @return AGREGADO con el carrito actualizado, o YA_EXISTE con el carrito sin cambios
     *         (en ese caso el archivo se reescribe con el mismo contenido).
     */
    public ResultadoAgregar agregarItemSiNoExiste(String idUsuario, Carrito.ItemCarrito nuevoItem) {
        return archivos.modificarLista(ARCHIVO, Carrito.class, lista -> {
            int posicion = posicionDe(lista, idUsuario);
            Carrito actual = posicion >= 0 ? lista.get(posicion) : null;

            // Copia limpia de los ítems: sin null (por si alguien editó el JSON a mano).
            List<Carrito.ItemCarrito> items = new ArrayList<>();
            if (actual != null && actual.items() != null) {
                for (Carrito.ItemCarrito item : actual.items()) {
                    if (item != null && item.idMueble() != null) {
                        items.add(item);
                    }
                }
            }

            boolean yaEsta = items.stream().anyMatch(i -> i.idMueble().equals(nuevoItem.idMueble()));
            if (yaEsta) {
                return new ResultadoAgregar(ResultadoAgregar.Estado.YA_EXISTE, actual);
            }

            items.add(nuevoItem);
            Carrito actualizado = new Carrito(idUsuario, items, Instant.now());
            if (posicion >= 0) {
                lista.set(posicion, actualizado);
            } else {
                lista.add(actualizado);
            }
            return new ResultadoAgregar(ResultadoAgregar.Estado.AGREGADO, actualizado);
        });
    }

    /**
     * Entrega los ítems del carrito del usuario a {@code procesar} y, SOLO si este termina sin error,
     * deja el carrito vacío (items [] y nunca null). Lo usa "Registrar Pedido".
     *
     * Todo ocurre bajo el candado de carritos.json: si el cliente hace doble clic en
     * "Registrar pedido", la segunda petición espera, encuentra el carrito ya vacío y no duplica el pedido.
     * Si {@code procesar} lanza una excepción (stock insuficiente, carrito vacío...), el archivo no se toca.
     *
     * @param procesar recibe una copia de los ítems (sin null) y devuelve el resultado (por ejemplo, el pedido).
     */
    public <R> R procesarItemsYVaciar(String idUsuario, Function<List<Carrito.ItemCarrito>, R> procesar) {
        return archivos.modificarLista(ARCHIVO, Carrito.class, lista -> {
            int posicion = posicionDe(lista, idUsuario);

            List<Carrito.ItemCarrito> items = new ArrayList<>();
            if (posicion >= 0 && lista.get(posicion).items() != null) {
                for (Carrito.ItemCarrito item : lista.get(posicion).items()) {
                    if (item != null && item.idMueble() != null) {
                        items.add(item);
                    }
                }
            }

            R resultado = procesar.apply(List.copyOf(items));

            if (posicion >= 0) {
                lista.set(posicion, new Carrito(idUsuario, new ArrayList<>(), Instant.now()));
            }
            return resultado;
        });
    }

    private static int posicionDe(List<Carrito> lista, String idUsuario) {
        for (int i = 0; i < lista.size(); i++) {
            if (idUsuario.equals(lista.get(i).idUsuario())) {
                return i;
            }
        }
        return -1;
    }
}
