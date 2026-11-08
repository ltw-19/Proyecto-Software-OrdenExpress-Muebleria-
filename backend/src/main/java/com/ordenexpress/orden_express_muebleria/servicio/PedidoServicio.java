package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionAlmacenamiento;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.modelo.EstadoPedido;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.modelo.Pedido;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Lógica de la épica Gestionar Pedido. Por ahora: Registrar Pedido (con Pre-orden en PDF y WhatsApp). */
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

    private Pedido buscarPedidoPropio(String idUsuario, String idPedido) {
        if (idPedido == null || idPedido.isBlank()) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "El pedido es obligatorio.");
        }
        Pedido pedido = pedidoRepositorio.buscarPorId(idPedido.trim())
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El pedido no existe."));
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
