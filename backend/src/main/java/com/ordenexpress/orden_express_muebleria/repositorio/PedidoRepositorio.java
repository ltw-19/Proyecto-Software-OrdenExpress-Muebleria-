package com.ordenexpress.orden_express_muebleria.repositorio;

import com.ordenexpress.orden_express_muebleria.modelo.Pedido;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Acceso a pedidos.json. Solo sabe leer y guardar pedidos; las reglas de negocio están en PedidoServicio. */
@Repository
public class PedidoRepositorio {

    private static final String ARCHIVO = "pedidos.json";

    /** Forma de los números de pedido: OE-0001, OE-0002... */
    private static final Pattern PATRON_NUMERO = Pattern.compile("^OE-(\\d{1,9})$");

    private final ServicioArchivoJson archivos;

    public PedidoRepositorio(ServicioArchivoJson archivos) {
        this.archivos = archivos;
    }

    public Optional<Pedido> buscarPorId(String id) {
        return archivos.leerLista(ARCHIVO, Pedido.class).stream()
                .filter(p -> id.equals(p.id()))
                .findFirst();
    }

    /** Todos los pedidos de pedidos.json (el filtro por cliente lo hace PedidoServicio). */
    public List<Pedido> listar() {
        return archivos.leerLista(ARCHIVO, Pedido.class);
    }

    /**
     * Reemplaza UN pedido por su versión modificada, bajo el candado de pedidos.json.
     * El cambio se calcula sobre el registro MÁS RECIENTE del archivo: si dos gestores
     * aprueban a la vez, el segundo ya ve el estado que dejó el primero.
     * Si {@code cambio} lanza una excepción (por ejemplo, "el pedido no está PENDIENTE"),
     * el archivo NO cambia.
     *
     * @return el pedido ya guardado, o vacío si no existe ningún pedido con ese id.
     */
    public Optional<Pedido> actualizar(String id, UnaryOperator<Pedido> cambio) {
        return archivos.modificarLista(ARCHIVO, Pedido.class, lista -> {
            for (int i = 0; i < lista.size(); i++) {
                if (id.equals(lista.get(i).id())) {
                    Pedido actualizado = cambio.apply(lista.get(i));
                    lista.set(i, actualizado);
                    return Optional.of(actualizado);
                }
            }
            return Optional.<Pedido>empty();
        });
    }

    /**
     * Agrega un pedido nuevo asignándole el siguiente número (OE-0001, OE-0002...).
     * El número es "el mayor existente + 1" y se calcula bajo el mismo candado que el guardado:
     * dos clientes que registran a la vez reciben números distintos.
     *
     * @param fabrica recibe el número ya calculado y devuelve el pedido completo a guardar.
     * @return el pedido guardado.
     */
    public Pedido agregarConNumeroNuevo(Function<String, Pedido> fabrica) {
        return archivos.modificarLista(ARCHIVO, Pedido.class, lista -> {
            int maximo = 0;
            for (Pedido existente : lista) {
                if (existente.numero() == null) {
                    continue;
                }
                Matcher coincidencia = PATRON_NUMERO.matcher(existente.numero());
                if (coincidencia.matches()) {
                    maximo = Math.max(maximo, Integer.parseInt(coincidencia.group(1)));
                }
            }
            Pedido nuevo = fabrica.apply(String.format(Locale.ROOT, "OE-%04d", maximo + 1));
            lista.add(nuevo);
            return nuevo;
        });
    }

    /**
     * Borra UN pedido de pedidos.json después de que {@code validar} lo apruebe, todo bajo el mismo
     * candado. Así nadie puede cambiar el pedido entre la revisión y el borrado (por ejemplo, que el
     * GESTOR lo apruebe justo cuando el cliente lo está eliminando).
     * Si {@code validar} lanza una excepción (sin permiso), el archivo NO cambia.
     *
     * @return el pedido borrado, o vacío si no existe ningún pedido con ese id.
     */
    public Optional<Pedido> eliminar(String id, Consumer<Pedido> validar) {
        return archivos.modificarLista(ARCHIVO, Pedido.class, lista -> {
            for (int i = 0; i < lista.size(); i++) {
                if (id.equals(lista.get(i).id())) {
                    Pedido encontrado = lista.get(i);
                    validar.accept(encontrado);
                    lista.remove(i);
                    return Optional.of(encontrado);
                }
            }
            return Optional.<Pedido>empty();
        });
    }

    /**
     * Borra un pedido por id. Solo lo usa PedidoServicio como COMPENSACIÓN cuando el pedido
     * se guardó pero no se pudo vaciar el carrito (para no dejar un pedido "huérfano"
     * que el cliente podría duplicar). No es la historia "Eliminar Pedido".
     */
    public void eliminarPorId(String id) {
        archivos.modificarLista(ARCHIVO, Pedido.class, lista -> lista.removeIf(p -> id.equals(p.id())));
    }
}
