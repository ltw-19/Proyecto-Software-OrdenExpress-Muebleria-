package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Eliminar Pedido con archivos JSON reales en una carpeta temporal. Stock de prueba: m1 = 5 unidades.
 * (El ADMIN lo frena @PreAuthorize en el controlador; se prueba en la colección de Postman.)
 */
class EliminarPedidoTest {

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
                        new BigDecimal("480.00"), 5)));
        ConversionMonedaServicio conversion = new ConversionMonedaServicio(new BigDecimal("40.00"));
        CarritoRepositorio carritos = new CarritoRepositorio(archivos);
        carritoServicio = new CarritoServicio(carritos, muebles, conversion);
        pedidoServicio = new PedidoServicio(new PedidoRepositorio(archivos), carritos, muebles, conversion,
                new PreordenPdfServicio(), "584120000000");
    }

    private PedidoRespuesta registrar(String cliente, int cantidad) {
        carritoServicio.agregarItem(cliente, new AgregarItemCarritoSolicitud("m1", cantidad));
        return pedidoServicio.registrar(cliente, ENTREGA);
    }

    private int codigoDe(Runnable accion) {
        return assertThrows(ExcepcionNegocio.class, accion::run).getEstado().value();
    }

    @Test
    void elClienteEliminaSuPedidoPendiente() {
        PedidoRespuesta pedido = registrar("cliente-1", 1);

        pedidoServicio.eliminar("cliente-1", Rol.CLIENTE, pedido.id());

        assertTrue(pedidoServicio.consultar("cliente-1", Rol.CLIENTE).isEmpty());
        assertEquals(404, codigoDe(() -> pedidoServicio.eliminar("cliente-1", Rol.CLIENTE, pedido.id())));
    }

    @Test
    void elClienteNoEliminaPedidosAjenosNiYaDecididos() {
        PedidoRespuesta ajeno = registrar("cliente-2", 1);
        assertEquals(403, codigoDe(() -> pedidoServicio.eliminar("cliente-1", Rol.CLIENTE, ajeno.id())));

        PedidoRespuesta aprobado = registrar("cliente-1", 1);
        pedidoServicio.aprobar(aprobado.id());
        var error = assertThrows(ExcepcionNegocio.class,
                () -> pedidoServicio.eliminar("cliente-1", Rol.CLIENTE, aprobado.id()));
        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        assertTrue(error.getMessage().contains("PENDIENTES"));

        PedidoRespuesta rechazado = registrar("cliente-1", 1);
        pedidoServicio.rechazar(rechazado.id());
        assertEquals(403, codigoDe(() -> pedidoServicio.eliminar("cliente-1", Rol.CLIENTE, rechazado.id())));

        assertEquals(3, pedidoServicio.consultar("gestor", Rol.GESTOR).size());   // nada se borró
    }

    @Test
    void elGestorEliminaCualquieraYNoSeDevuelveStock() {
        PedidoRespuesta aprobado = registrar("cliente-1", 2);
        pedidoServicio.aprobar(aprobado.id());
        assertEquals(3, muebles.buscarPorId("m1").orElseThrow().unidades());

        pedidoServicio.eliminar("gestor", Rol.GESTOR, aprobado.id());

        assertTrue(pedidoServicio.consultar("gestor", Rol.GESTOR).isEmpty());
        assertEquals(3, muebles.buscarPorId("m1").orElseThrow().unidades());     // el stock no regresa
    }

    @Test
    void pedidoInexistenteDa404() {
        assertEquals(404, codigoDe(() -> pedidoServicio.eliminar("gestor", Rol.GESTOR, "no-existe")));
    }
}
