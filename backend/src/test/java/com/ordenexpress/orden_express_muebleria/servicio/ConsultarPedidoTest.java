package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.EstadoPedido;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.repositorio.CarritoRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.PedidoRepositorio;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Consultar Pedido (lista, detalle) y Aprobar/Rechazar, con archivos JSON reales en una carpeta temporal.
 * Stock de prueba: m1 = 5 unidades, m2 = 1 unidad.
 */
class ConsultarPedidoTest {

    @TempDir
    Path carpeta;

    private CarritoServicio carritoServicio;
    private PedidoServicio pedidoServicio;
    private MuebleRepositorio muebles;

    private static final RegistrarPedidoSolicitud ENTREGA =
            new RegistrarPedidoSolicitud("Ana", "Pérez", "04121234567", "Av. Principal, Caracas");

    @BeforeEach
    void preparar() {
        ServicioArchivoJson archivos = new ServicioArchivoJson(carpeta.toString());
        archivos.inicializar();
        muebles = new MuebleRepositorio(archivos);
        muebles.guardarTodos(List.of(
                new Mueble("m1", "M-001", "Sofá Alba", "Sala", "Sofá", null, "Madera", "https://x/a.png",
                        new BigDecimal("480.00"), 5),
                new Mueble("m2", "M-002", "Silla Roble", "Comedor", "Silla", null, "Roble", "https://x/b.png",
                        new BigDecimal("33.33"), 1)));
        ConversionMonedaServicio conversion = new ConversionMonedaServicio(new BigDecimal("40.00"));
        CarritoRepositorio carritos = new CarritoRepositorio(archivos);
        carritoServicio = new CarritoServicio(carritos, muebles, conversion);
        pedidoServicio = new PedidoServicio(new PedidoRepositorio(archivos), carritos, muebles, conversion,
                new PreordenPdfServicio(), "584120000000");
    }

    private PedidoRespuesta registrar(String cliente, String idMueble, int cantidad) {
        carritoServicio.agregarItem(cliente, new AgregarItemCarritoSolicitud(idMueble, cantidad));
        return pedidoServicio.registrar(cliente, ENTREGA);
    }

    private int unidades(String idMueble) {
        return muebles.buscarPorId(idMueble).orElseThrow().unidades();
    }

    @Test
    void elClienteSoloVeLosSuyosYElGestorVeTodosDelMasRecienteAlMasAntiguo() {
        registrar("cliente-1", "m1", 1);
        registrar("cliente-2", "m1", 1);

        List<PedidoRespuesta> delCliente1 = pedidoServicio.consultar("cliente-1", Rol.CLIENTE);
        assertEquals(1, delCliente1.size());
        assertEquals("OE-0001", delCliente1.get(0).numero());

        List<PedidoRespuesta> bandeja = pedidoServicio.consultar("gestor", Rol.GESTOR);
        assertEquals(List.of("OE-0002", "OE-0001"), bandeja.stream().map(PedidoRespuesta::numero).toList());
        assertEquals(2, pedidoServicio.consultar("admin", Rol.ADMIN).size());
    }

    @Test
    void detalleRespetaPermisos() {
        PedidoRespuesta pedido = registrar("cliente-1", "m1", 1);

        assertEquals("OE-0001", pedidoServicio.consultarDetalle("cliente-1", Rol.CLIENTE, pedido.id()).numero());
        assertEquals("OE-0001", pedidoServicio.consultarDetalle("admin", Rol.ADMIN, pedido.id()).numero());

        var ajeno = assertThrows(ExcepcionNegocio.class,
                () -> pedidoServicio.consultarDetalle("cliente-2", Rol.CLIENTE, pedido.id()));
        assertEquals(HttpStatus.FORBIDDEN, ajeno.getEstado());
        var noExiste = assertThrows(ExcepcionNegocio.class,
                () -> pedidoServicio.consultarDetalle("gestor", Rol.GESTOR, "no-existe"));
        assertEquals(HttpStatus.NOT_FOUND, noExiste.getEstado());
    }

    @Test
    void aprobarDescuentaElStockUnaSolaVez() {
        PedidoRespuesta pedido = registrar("cliente-1", "m1", 2);
        assertEquals(5, unidades("m1"));                     // registrar NO descuenta

        PedidoRespuesta aprobado = pedidoServicio.aprobar(pedido.id());
        assertEquals(EstadoPedido.APROBADO, aprobado.estado());
        assertEquals(pedido.totalUsd(), aprobado.totalUsd()); // montos congelados intactos
        assertEquals(3, unidades("m1"));

        var otraVez = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.aprobar(pedido.id()));
        assertEquals(HttpStatus.CONFLICT, otraVez.getEstado());
        assertEquals(3, unidades("m1"));                     // el doble clic no descuenta dos veces
    }

    @Test
    void sinStockNoApruebaNiDescuentaNada() {
        // Dos clientes piden la única silla; se aprueba el primero.
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m1", 1));
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m2", 1));
        PedidoRespuesta conSilla = pedidoServicio.registrar("cliente-1", ENTREGA);
        PedidoRespuesta otro = registrar("cliente-2", "m2", 1);
        pedidoServicio.aprobar(otro.id());
        assertEquals(0, unidades("m2"));

        var error = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.aprobar(conSilla.id()));
        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        assertEquals("Stock insuficiente", error.getMessage());
        assertEquals(5, unidades("m1"));                     // todo o nada: el sofá tampoco se descontó
        assertEquals(EstadoPedido.PENDIENTE,
                pedidoServicio.consultarDetalle("cliente-1", Rol.CLIENTE, conSilla.id()).estado());
    }

    @Test
    void rechazarNoTocaElStockYSoloDesdePendiente() {
        PedidoRespuesta pedido = registrar("cliente-1", "m1", 2);

        assertEquals(EstadoPedido.RECHAZADO, pedidoServicio.rechazar(pedido.id()).estado());
        assertEquals(5, unidades("m1"));

        var aprobarRechazado = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.aprobar(pedido.id()));
        assertEquals(HttpStatus.CONFLICT, aprobarRechazado.getEstado());
        var noExiste = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.rechazar("no-existe"));
        assertEquals(HttpStatus.NOT_FOUND, noExiste.getEstado());
    }
}
