package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionAlmacenamiento;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.modelo.EstadoPedido;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.modelo.Pedido;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.repositorio.CarritoRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.PedidoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lógica de la épica Gestionar Pedido: Registrar Pedido (con Pre-orden en PDF y WhatsApp)
 * Consultar Pedido (lista, detalle y Aprobar/Rechazar del GESTOR) y Eliminar Pedido.
 */
@Service
public class PedidoServicio {

    private static final Logger registro = LoggerFactory.getLogger(PedidoServicio.class);

    private final PedidoRepositorio pedidoRepositorio;
    private final CarritoRepositorio carritoRepositorio;
    private final MuebleRepositorio muebleRepositorio;
    private final ConversionMonedaServicio conversionMoneda;
    private final PreordenPdfServicio preordenPdf;
    private final String numeroWhatsapp;

    public PedidoServicio(PedidoRepositorio pedidoRepositorio, CarritoRepositorio carritoRepositorio,
                          MuebleRepositorio muebleRepositorio, ConversionMonedaServicio conversionMoneda,
                          PreordenPdfServicio preordenPdf,
                          @Value("${app.whatsapp.numero}") String numeroWhatsapp) {
        if (numeroWhatsapp == null || !numeroWhatsapp.matches("^[0-9]{10,15}$")) {
            throw new IllegalStateException(
                    "La propiedad app.whatsapp.numero debe tener de 10 a 15 dígitos, sin '+' ni espacios.");
        }
        this.pedidoRepositorio = pedidoRepositorio;
        this.carritoRepositorio = carritoRepositorio;
        this.muebleRepositorio = muebleRepositorio;
        this.conversionMoneda = conversionMoneda;
        this.preordenPdf = preordenPdf;
        this.numeroWhatsapp = numeroWhatsapp;
    }

    // ------------------------------------------------------------------
    // Registrar Pedido
    // ------------------------------------------------------------------

    /**
     * Historia: REGISTRAR PEDIDO (CLIENTE).
     * Algoritmo:
     *  1. Limpia los datos de entrega (espacios sobrantes). El DTO ya los validó con @Valid.
     *  2. Bajo el candado de carritos.json toma los ítems del carrito del cliente (id del token).
     *     Si el carrito está vacío → 409.
     *  3. Para cada ítem lee el mueble en muebles.json:
     *       - si ya no existe o no tiene precio → 409;
     *       - si la cantidad supera las unidades en stock → 409 "Stock insuficiente".
     *     (El stock NO se descuenta aquí: se descuenta cuando el GESTOR aprueba.)
     *  4. Congela la tasa BCV del momento y calcula con BigDecimal (2 decimales, HALF_UP):
     *     subtotalUsd = precioUsd × cantidad; totalUsd = Σ subtotalUsd; los Bs. = monto en USD × tasa
     *     (el total en Bs. sale del total en USD, no de sumar Bs. redondeados).
     *  5. Guarda el pedido en pedidos.json con número OE-#### (mayor + 1), estado PENDIENTE y timestamp.
     *  6. Vacía el carrito. Si vaciarlo falla, BORRA el pedido recién guardado (compensación) y responde 500,
     *     para que no quede un pedido registrado con el carrito todavía lleno.
     *  7. Responde el pedido con el enlace de WhatsApp ya armado.
     *
     * Doble clic: la segunda petición espera el candado del carrito, lo encuentra vacío y recibe 409.
     *
     * @param idUsuario id del cliente, tomado del token.
     */
    public PedidoRespuesta registrar(String idUsuario, RegistrarPedidoSolicitud solicitud) {
        Pedido.DatosEntrega entrega = limpiarEntrega(solicitud);

        AtomicReference<Pedido> pedidoGuardado = new AtomicReference<>();
        try {
            Pedido pedido = carritoRepositorio.procesarItemsYVaciar(idUsuario, items -> {
                BigDecimal tasa = conversionMoneda.getTasaBcv();   // se lee UNA vez: tasa congelada del pedido
                List<Pedido.ItemPedido> itemsPedido = congelarItems(items, tasa);
                Pedido nuevo = guardarPedido(idUsuario, itemsPedido, tasa, entrega);
                pedidoGuardado.set(nuevo);
                return nuevo;
            });
            return aRespuesta(pedido);
        } catch (ExcepcionAlmacenamiento e) {
            compensar(pedidoGuardado.get());
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // Descargar Pre-orden (PDF)
    // ------------------------------------------------------------------

    /**
     * Genera la Pre-orden en PDF de un pedido YA registrado. Se puede descargar las veces que
     * se quiera: descargarla no registra nada ni duplica el pedido.
     * Solo el cliente dueño del pedido: no existe → 404; es de otra persona → 403.
     */
    public Preorden generarPreorden(String idUsuario, String idPedido) {
        Pedido pedido = buscarPedidoPropio(idUsuario, idPedido);
        return new Preorden(pedido.numero(), preordenPdf.generar(pedido));
    }

    /** PDF listo para enviar: número del pedido (para el nombre del archivo) y bytes. */
    public record Preorden(String numero, byte[] contenido) {
    }

    // ------------------------------------------------------------------
    // Consultar Pedido (lista y detalle)
    // ------------------------------------------------------------------

    /**
     * Historia: CONSULTAR PEDIDO (TODOS los roles).
     * Algoritmo:
     *  1. Lee pedidos.json.
     *  2. CLIENTE → se queda solo con los pedidos cuyo idUsuario es el suyo (id del token).
     *     ADMIN y GESTOR → bandeja global: todos los pedidos.
     *  3. Ordena del más reciente al más antiguo (por timestamp).
     *  4. Responde los montos CONGELADOS de cada pedido (no se recalculan con la tasa actual).
     *
     * @param idUsuario id de quien consulta, tomado del token.
     * @param rol       rol de quien consulta, releído de usuarios.json por el filtro JWT.
     */
    public List<PedidoRespuesta> consultar(String idUsuario, Rol rol) {
        return pedidoRepositorio.listar().stream()
                .filter(pedido -> rol != Rol.CLIENTE || idUsuario.equals(pedido.idUsuario()))
                .sorted(Comparator.comparing(Pedido::timestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * Detalle de un pedido. El CLIENTE solo ve los suyos (de otra persona → 403);
     * ADMIN y GESTOR ven cualquiera. No existe → 404.
     */
    public PedidoRespuesta consultarDetalle(String idUsuario, Rol rol, String idPedido) {
        Pedido pedido = rol == Rol.CLIENTE
                ? buscarPedidoPropio(idUsuario, idPedido)
                : buscarPedido(idPedido);
        return aRespuesta(pedido);
    }

    // ------------------------------------------------------------------
    // Aprobar / Rechazar (parte de Consultar Pedido: bandeja del GESTOR)
    // ------------------------------------------------------------------

    /**
     * APROBAR PEDIDO (solo GESTOR; el permiso lo revisa el controlador con @PreAuthorize).
     * Algoritmo (toca DOS archivos, por eso el orden y la compensación):
     *  1. Toma el candado de pedidos.json y lee la versión más reciente del pedido.
     *     No existe → 404. No está PENDIENTE → 409 (así un doble clic no descuenta dos veces).
     *  2. Bajo el candado de muebles.json revisa TODOS los ítems:
     *       - si un mueble ya no existe → 409;
     *       - si la cantidad pedida supera las unidades actuales → 409 "Stock insuficiente".
     *     Si alguno falla, no se descuenta nada (todo el pedido o nada) y el pedido sigue PENDIENTE.
     *  3. Si todos alcanzan, resta las unidades y guarda muebles.json.
     *  4. Cambia el estado a APROBADO y guarda pedidos.json.
     *  5. Compensación: si falla guardar pedidos.json (paso 4), DEVUELVE las unidades descontadas
     *     en el paso 3 y responde 500, para que el stock no quede rebajado con el pedido PENDIENTE.
     */
    public PedidoRespuesta aprobar(String idPedido) {
        String id = exigirId(idPedido);
        AtomicReference<Pedido> stockDescontado = new AtomicReference<>();
        try {
            Pedido aprobado = pedidoRepositorio.actualizar(id, actual -> {
                exigirPendiente(actual, "aprobar");
                descontarStock(actual);
                stockDescontado.set(actual);
                return conEstado(actual, EstadoPedido.APROBADO);
            }).orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El pedido no existe."));
            return aRespuesta(aprobado);
        } catch (ExcepcionAlmacenamiento e) {
            devolverStock(stockDescontado.get());
            throw e;
        }
    }

    /**
     * RECHAZAR PEDIDO (solo GESTOR). Solo desde PENDIENTE (si no → 409).
     * No toca el stock: nunca se descontó, porque solo se descuenta al aprobar.
     */
    public PedidoRespuesta rechazar(String idPedido) {
        String id = exigirId(idPedido);
        Pedido rechazado = pedidoRepositorio.actualizar(id, actual -> {
            exigirPendiente(actual, "rechazar");
            return conEstado(actual, EstadoPedido.RECHAZADO);
        }).orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El pedido no existe."));
        return aRespuesta(rechazado);
    }

    // ------------------------------------------------------------------
    // Eliminar Pedido
    // ------------------------------------------------------------------

    /**
     * Historia: ELIMINAR PEDIDO (CLIENTE y GESTOR; el ADMIN no, lo frena @PreAuthorize en el controlador).
     * Algoritmo:
     *  1. Toma el candado de pedidos.json y busca el pedido → no existe: 404.
     *  2. Si quien elimina es CLIENTE:
     *       - el pedido debe ser suyo (id del token) → si no, 403;
     *       - debe estar PENDIENTE → si ya fue aprobado o rechazado, 403.
     *     El GESTOR puede eliminar cualquier pedido, en cualquier estado.
     *  3. Borra el nodo de pedidos.json (bajo el mismo candado de la revisión).
     *  4. NO devuelve stock: si el pedido estaba APROBADO, las unidades descontadas no regresan.
     *
     * @param idUsuario id de quien elimina, tomado del token.
     * @param rol       rol de quien elimina (CLIENTE o GESTOR).
     */
    public void eliminar(String idUsuario, Rol rol, String idPedido) {
        String id = exigirId(idPedido);
        Pedido eliminado = pedidoRepositorio.eliminar(id, pedido -> {
            if (rol == Rol.CLIENTE) {
                if (!idUsuario.equals(pedido.idUsuario())) {
                    throw new ExcepcionNegocio(HttpStatus.FORBIDDEN, "No tienes permisos para eliminar este pedido.");
                }
                if (pedido.estado() != EstadoPedido.PENDIENTE) {
                    throw new ExcepcionNegocio(HttpStatus.FORBIDDEN,
                            "Solo puedes eliminar pedidos PENDIENTES. Este pedido ya está " + pedido.estado() + ".");
                }
            }
        }).orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El pedido no existe."));
        registro.info("Pedido {} eliminado por un usuario con rol {}.", eliminado.numero(), rol);
    }

    /** Solo se aprueba o rechaza un pedido PENDIENTE: un pedido ya decidido no cambia. */
    private static void exigirPendiente(Pedido pedido, String accion) {
        if (pedido.estado() != EstadoPedido.PENDIENTE) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                    "Solo se puede " + accion + " un pedido PENDIENTE. Este pedido ya está " + pedido.estado() + ".");
        }
    }

    /**
     * Pasos 2 y 3 de Aprobar: valida y descuenta el stock de todos los ítems bajo el candado
     * de muebles.json. Primero valida TODO y después resta: si algo falla, se lanza la
     * excepción antes de guardar y muebles.json queda igual.
     */
    private void descontarStock(Pedido pedido) {
        Map<String, Integer> cantidades = cantidadesPorMueble(pedido);
        muebleRepositorio.modificar(lista -> {
            Map<String, Mueble> mueblesPorId = new HashMap<>();
            for (Mueble mueble : lista) {
                mueblesPorId.put(mueble.id(), mueble);
            }
            for (Map.Entry<String, Integer> pedidoMueble : cantidades.entrySet()) {
                Mueble mueble = mueblesPorId.get(pedidoMueble.getKey());
                if (mueble == null) {
                    throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                            "Uno de los muebles del pedido ya no está en el catálogo. No se puede aprobar.");
                }
                int stock = mueble.unidades() == null ? 0 : mueble.unidades();
                if (pedidoMueble.getValue() > stock) {
                    throw new ExcepcionNegocio(HttpStatus.CONFLICT, "Stock insuficiente");
                }
            }
            lista.replaceAll(mueble -> cantidades.containsKey(mueble.id())
                    ? conUnidades(mueble, mueble.unidades() - cantidades.get(mueble.id()))
                    : mueble);
            return null;
        });
    }

    /** Paso 5 de Aprobar (compensación): suma de vuelta las unidades que se descontaron. */
    private void devolverStock(Pedido pedido) {
        if (pedido == null) {
            return;   // el stock no llegó a descontarse: no hay nada que devolver
        }
        Map<String, Integer> cantidades = cantidadesPorMueble(pedido);
        try {
            muebleRepositorio.modificar(lista -> {
                lista.replaceAll(mueble -> cantidades.containsKey(mueble.id())
                        ? conUnidades(mueble, (mueble.unidades() == null ? 0 : mueble.unidades())
                                + cantidades.get(mueble.id()))
                        : mueble);
                return null;
            });
            registro.warn("Se devolvió el stock del pedido {} porque no se pudo guardar su aprobación.",
                    pedido.numero());
        } catch (ExcepcionAlmacenamiento e) {
            registro.error("No se pudo devolver el stock del pedido {} tras fallar su aprobación.",
                    pedido.numero(), e);
        }
    }

    /** Suma la cantidad pedida de cada mueble (por si un mismo mueble apareciera en dos ítems). */
    private static Map<String, Integer> cantidadesPorMueble(Pedido pedido) {
        Map<String, Integer> cantidades = new HashMap<>();
        for (Pedido.ItemPedido item : pedido.items()) {
            cantidades.merge(item.idMueble(), item.cantidad(), Integer::sum);
        }
        return cantidades;
    }

    private static Mueble conUnidades(Mueble m, int unidades) {
        return new Mueble(m.id(), m.codigo(), m.nombre(), m.categoria(), m.descripcion(), m.dimensiones(),
                m.material(), m.urlFoto(), m.precioUsd(), unidades);
    }

    /** Copia del pedido con otro estado. Todo lo demás (montos, tasa, timestamp) queda igual. */
    private static Pedido conEstado(Pedido p, EstadoPedido estado) {
        return new Pedido(p.id(), p.numero(), p.idUsuario(), p.items(), p.tasaBcv(), p.totalUsd(),
                p.totalBs(), p.entrega(), estado, p.timestamp());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Pedido.DatosEntrega limpiarEntrega(RegistrarPedidoSolicitud solicitud) {
        if (solicitud == null) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "Los datos de entrega son obligatorios.");
        }
        return new Pedido.DatosEntrega(
                recortar(solicitud.nombre()),
                recortar(solicitud.apellido()),
                recortar(solicitud.telefono()),
                recortar(solicitud.direccion()));
    }

    private static String recortar(String texto) {
        return texto == null ? null : texto.trim();
    }

    /**
     * Pasos 3 y 4: valida stock y congela precios y tasa. Lanza 409 si algo no cuadra
     * (en ese caso el carrito y los pedidos quedan sin cambios).
     */
    private List<Pedido.ItemPedido> congelarItems(List<Carrito.ItemCarrito> items, BigDecimal tasa) {
        if (items.isEmpty()) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                    "Tu carrito está vacío. Añade al menos un mueble antes de registrar el pedido.");
        }

        Map<String, Mueble> mueblesPorId = new HashMap<>();
        for (Mueble mueble : muebleRepositorio.listar()) {
            mueblesPorId.put(mueble.id(), mueble);
        }

        List<Pedido.ItemPedido> resultado = new ArrayList<>();
        for (Carrito.ItemCarrito item : items) {
            if (item.cantidad() == null || item.cantidad() < 1) {
                continue;   // ítem dañado (editado a mano en el JSON): se ignora, igual que en Consultar Carrito
            }
            Mueble mueble = mueblesPorId.get(item.idMueble());
            if (mueble == null || mueble.precioUsd() == null) {
                throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                        "Uno de los muebles de tu carrito ya no está en el catálogo. Elimínalo para continuar.");
            }
            int stock = mueble.unidades() == null ? 0 : mueble.unidades();
            if (item.cantidad() > stock) {
                throw new ExcepcionNegocio(HttpStatus.CONFLICT, "Stock insuficiente");
            }

            BigDecimal precioUsd = mueble.precioUsd().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalUsd = precioUsd.multiply(BigDecimal.valueOf(item.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
            resultado.add(new Pedido.ItemPedido(
                    mueble.id(),
                    mueble.codigo(),
                    mueble.nombre(),
                    item.cantidad(),
                    precioUsd,
                    ConversionMonedaServicio.usdABolivares(precioUsd, tasa),
                    subtotalUsd,
                    ConversionMonedaServicio.usdABolivares(subtotalUsd, tasa)));
        }

        if (resultado.isEmpty()) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                    "Tu carrito está vacío. Añade al menos un mueble antes de registrar el pedido.");
        }
        return resultado;
    }

    /** Paso 5: guarda el pedido con número nuevo, usando la MISMA tasa con que se calcularon los ítems. */
    private Pedido guardarPedido(String idUsuario, List<Pedido.ItemPedido> items, BigDecimal tasa,
                                 Pedido.DatosEntrega entrega) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Pedido.ItemPedido item : items) {
            suma = suma.add(item.subtotalUsd());
        }
        BigDecimal totalUsd = suma.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBs = ConversionMonedaServicio.usdABolivares(totalUsd, tasa);

        String id = UUID.randomUUID().toString();
        return pedidoRepositorio.agregarConNumeroNuevo(numero -> new Pedido(
                id, numero, idUsuario, List.copyOf(items), tasa, totalUsd, totalBs,
                entrega, EstadoPedido.PENDIENTE, Instant.now()));
    }

    /** Paso 6 (compensación): si el carrito no se pudo vaciar, se deshace el pedido guardado. */
    private void compensar(Pedido pedido) {
        if (pedido == null) {
            return;   // falló antes de guardar el pedido: no hay nada que deshacer
        }
        try {
            pedidoRepositorio.eliminarPorId(pedido.id());
            registro.warn("Se deshizo el pedido {} porque no se pudo vaciar el carrito.", pedido.numero());
        } catch (ExcepcionAlmacenamiento e) {
            registro.error("No se pudo deshacer el pedido {} tras fallar el vaciado del carrito.", pedido.numero(), e);
        }
    }

    private static String exigirId(String idPedido) {
        if (idPedido == null || idPedido.isBlank()) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "El pedido es obligatorio.");
        }
        return idPedido.trim();
    }

    private Pedido buscarPedido(String idPedido) {
        return pedidoRepositorio.buscarPorId(exigirId(idPedido))
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El pedido no existe."));
    }

    private Pedido buscarPedidoPropio(String idUsuario, String idPedido) {
        Pedido pedido = buscarPedido(idPedido);
        if (!idUsuario.equals(pedido.idUsuario())) {
            throw new ExcepcionNegocio(HttpStatus.FORBIDDEN, "No tienes permisos para ver este pedido.");
        }
        return pedido;
    }

    private PedidoRespuesta aRespuesta(Pedido pedido) {
        return new PedidoRespuesta(
                pedido.id(),
                pedido.numero(),
                pedido.estado(),
                pedido.timestamp(),
                pedido.items(),
                pedido.tasaBcv(),
                pedido.totalUsd(),
                pedido.totalBs(),
                pedido.entrega(),
                enlaceWhatsapp(pedido));
    }

    /**
     * Arma el enlace https://wa.me/<número del gestor>?text=<mensaje> con el mensaje pre-estructurado.
     * Los montos son los congelados del pedido, con formato venezolano (1.234,50).
     */
    String enlaceWhatsapp(Pedido pedido) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-VE"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        formato.setRoundingMode(RoundingMode.HALF_UP);

        Pedido.DatosEntrega entrega = pedido.entrega();
        StringBuilder mensaje = new StringBuilder()
                .append("Hola, quiero confirmar mi pre-orden ").append(pedido.numero())
                .append(" de Galería de Casa.\n")
                .append("Cliente: ").append(entrega.nombre()).append(' ').append(entrega.apellido()).append('\n')
                .append("Teléfono: ").append(entrega.telefono()).append('\n')
                .append("Dirección: ").append(entrega.direccion()).append('\n')
                .append("Productos:\n");
        for (Pedido.ItemPedido item : pedido.items()) {
            mensaje.append("- ").append(item.cantidad()).append(" x ").append(item.nombre())
                    .append(" (").append(item.codigo()).append("): USD ")
                    .append(formato.format(item.subtotalUsd())).append('\n');
        }
        mensaje.append("Total: USD ").append(formato.format(pedido.totalUsd()))
                .append(" / Bs. ").append(formato.format(pedido.totalBs())).append('\n')
                .append("Tasa: Bs. ").append(formato.format(pedido.tasaBcv())).append(" por USD");

        String texto = URLEncoder.encode(mensaje.toString(), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + numeroWhatsapp + "?text=" + texto;
    }
}
