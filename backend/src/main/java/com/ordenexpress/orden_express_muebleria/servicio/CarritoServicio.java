package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.CarritoRespuesta;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.repositorio.CarritoRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lógica de la épica Gestionar Carrito: Añadir ítem, Consultar y Eliminar ítem. */
@Service
public class CarritoServicio {

    /** Mismo tope que @Max en AgregarItemCarritoSolicitud y que las unidades de un mueble. */
    private static final int CANTIDAD_MAXIMA = 100000;

    private final CarritoRepositorio carritoRepositorio;
    private final MuebleRepositorio muebleRepositorio;
    private final ConversionMonedaServicio conversionMoneda;

    public CarritoServicio(CarritoRepositorio carritoRepositorio, MuebleRepositorio muebleRepositorio,
                           ConversionMonedaServicio conversionMoneda) {
        this.carritoRepositorio = carritoRepositorio;
        this.muebleRepositorio = muebleRepositorio;
        this.conversionMoneda = conversionMoneda;
    }

    // ------------------------------------------------------------------
    // Añadir ítem al Carrito
    // ------------------------------------------------------------------

    /**
     * Historia: AÑADIR ÍTEM AL CARRITO (CLIENTE).
     * Algoritmo:
     *  1. Verifica los datos: idMueble no vacío y cantidad no nula, entera, entre 1 y 100000 → si no, 400.
     *     (El DTO ya lo valida con @Valid; se repite aquí para que el stock NUNCA se evalúe con una
     *     cantidad inválida, aunque alguien llame al servicio sin pasar por el controlador.)
     *  2. Busca el mueble en muebles.json; si no existe → 404.
     *  3. Si la cantidad pedida es mayor que las unidades en stock → 409 "Stock insuficiente".
     *     (El stock NO se descuenta aquí: se descuenta cuando el GESTOR aprueba el pedido.)
     *  4. Pide al repositorio que añada el ítem al carrito del usuario (lo crea si no existe),
     *     bajo un candado. Si el mueble ya estaba en el carrito → 409 (no hay − / +: eso es Sprint 2).
     *  5. Responde con el carrito completo y sus montos calculados con los precios actuales y la tasa BCV.
     *
     * @param idUsuario id del cliente, tomado del token.
     */
    public CarritoRespuesta agregarItem(String idUsuario, AgregarItemCarritoSolicitud solicitud) {
        // 1. Verificar datos (antes de tocar el stock).
        if (solicitud == null || solicitud.idMueble() == null || solicitud.idMueble().isBlank()) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "El mueble es obligatorio.");
        }
        int cantidad = validarCantidad(solicitud.cantidad());
        String idMueble = solicitud.idMueble().trim();

        Mueble mueble = muebleRepositorio.buscarPorId(idMueble)
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El mueble no existe."));

        int stock = mueble.unidades() == null ? 0 : mueble.unidades();
        if (cantidad > stock) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT, "Stock insuficiente");
        }

        var resultado = carritoRepositorio.agregarItemSiNoExiste(
                idUsuario, new Carrito.ItemCarrito(idMueble, cantidad, Instant.now()));

        if (resultado.estado() == CarritoRepositorio.ResultadoAgregar.Estado.YA_EXISTE) {
            throw new ExcepcionNegocio(HttpStatus.CONFLICT,
                    "Este mueble ya está en tu carrito. Elimínalo para añadirlo con otra cantidad.");
        }
        return aRespuesta(resultado.carrito());
    }

    // ------------------------------------------------------------------
    // Consultar Carrito
    // ------------------------------------------------------------------

    /**
     * Historia: CONSULTAR CARRITO (CLIENTE).
     * Algoritmo:
     *  1. Busca en carritos.json el carrito del cliente (el id sale del token).
     *  2. Si todavía no tiene carrito, responde un carrito vacío (items [], totales 0.00).
     *     No se crea nada en el archivo: consultar solo lee.
     *  3. Para cada ítem toma el precio ACTUAL de muebles.json y calcula subtotales y totales
     *     (USD y Bs. con la tasa BCV). Indica si el mueble sigue disponible (sin revelar la cifra de stock).
     *
     * @param idUsuario id del cliente, tomado del token.
     */
    public CarritoRespuesta consultar(String idUsuario) {
        Carrito carrito = carritoRepositorio.buscarPorUsuario(idUsuario)
                .orElseGet(() -> new Carrito(idUsuario, List.of(), null));
        return aRespuesta(carrito);
    }

    // ------------------------------------------------------------------
    // Eliminar ítem del Carrito
    // ------------------------------------------------------------------

    /**
     * Historia: ELIMINAR ÍTEM DEL CARRITO (CLIENTE).
     * Algoritmo:
     *  1. Verifica que venga el id del mueble → si no, 400.
     *  2. Pide al repositorio quitar ese mueble del carrito del cliente (el id sale del token),
     *     bajo el candado de carritos.json.
     *  3. Si el cliente no tiene carrito o ese mueble no está en él → 404 (también en el doble clic).
     *     No se exige que el mueble siga en muebles.json: así se puede quitar uno que ya no existe.
     *  4. Responde con el carrito actualizado y los totales recalculados con los precios actuales
     *     y la tasa BCV. Si era el último ítem, el carrito queda vacío (items [], totales 0.00).
     *     El stock no cambia (nunca se descontó al añadir).
     *
     * @param idUsuario id del cliente, tomado del token.
     * @param idMueble  id (UUID) del mueble a quitar, tomado de la URL.
     */
    public CarritoRespuesta eliminarItem(String idUsuario, String idMueble) {
        if (idMueble == null || idMueble.isBlank()) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "El mueble es obligatorio.");
        }
        Carrito actualizado = carritoRepositorio.eliminarItem(idUsuario, idMueble.trim())
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "Este mueble no está en tu carrito."));
        return aRespuesta(actualizado);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Validar Cantidad: no nula y entre 1 y CANTIDAD_MAXIMA. Devuelve la cantidad ya como int.
     * (El formato "entero, sin decimales ni comillas" lo garantiza DeserializadorCantidadEntera.)
     */
    private static int validarCantidad(Integer cantidad) {
        if (cantidad == null) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "La cantidad es obligatoria.");
        }
        if (cantidad < 1) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "La cantidad debe ser al menos 1.");
        }
        if (cantidad > CANTIDAD_MAXIMA) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "La cantidad admite máximo 100000.");
        }
        return cantidad;
    }

    /**
     * Arma la respuesta del carrito con los precios ACTUALES de muebles.json.
     * subtotalUsd = precioUsd × cantidad; totalUsd = suma de subtotales;
     * los bolívares se calculan convirtiendo cada monto en USD (nunca sumando Bs. ya redondeados).
     */
    private CarritoRespuesta aRespuesta(Carrito carrito) {
        Map<String, Mueble> mueblesPorId = new HashMap<>();
        for (Mueble mueble : muebleRepositorio.listar()) {
            mueblesPorId.put(mueble.id(), mueble);
        }

        List<CarritoRespuesta.ItemCarritoRespuesta> items = new ArrayList<>();
        BigDecimal totalUsd = BigDecimal.ZERO;

        List<Carrito.ItemCarrito> guardados = carrito.items() == null ? List.of() : carrito.items();
        for (Carrito.ItemCarrito item : guardados) {
            // Ítems dañados (por ejemplo, editados a mano en el JSON) se ignoran en vez de romper la respuesta.
            if (item == null || item.idMueble() == null || item.cantidad() == null || item.cantidad() < 1) {
                continue;
            }
            Mueble mueble = mueblesPorId.get(item.idMueble());
            if (mueble == null || mueble.precioUsd() == null) {
                continue;   // mueble que ya no existe en el catálogo: no se puede cotizar
            }
            // Igual que en el catálogo: el CLIENTE solo sabe si hay o no, nunca la cifra.
            boolean disponible = mueble.unidades() != null && mueble.unidades() > 0;
            BigDecimal subtotalUsd = mueble.precioUsd()
                    .multiply(BigDecimal.valueOf(item.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalUsd = totalUsd.add(subtotalUsd);

            items.add(new CarritoRespuesta.ItemCarritoRespuesta(
                    mueble.id(),
                    mueble.codigo(),
                    mueble.nombre(),
                    mueble.urlFoto(),
                    mueble.precioUsd(),
                    conversionMoneda.usdABolivares(mueble.precioUsd()),
                    item.cantidad(),
                    subtotalUsd,
                    conversionMoneda.usdABolivares(subtotalUsd),
                    disponible));
        }

        totalUsd = totalUsd.setScale(2, RoundingMode.HALF_UP);
        return new CarritoRespuesta(items, items.size(), totalUsd, conversionMoneda.usdABolivares(totalUsd));
    }
}
