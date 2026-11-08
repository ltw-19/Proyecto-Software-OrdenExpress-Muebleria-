package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.EstadoPedido;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
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
 * Registrar Pedido de punta a punta en el servicio, con archivos JSON reales en una carpeta temporal.
 * Tasa de prueba: 40.00 Bs./USD.
 */
class RegistrarPedidoTest {

    @TempDir
    Path carpeta;

    private CarritoServicio carritoServicio;
    private PedidoServicio pedidoServicio;
    private ServicioArchivoJson archivos;

    private static final RegistrarPedidoSolicitud ENTREGA =
            new RegistrarPedidoSolicitud(" Ana ", "Pérez", "04121234567", "Av. Principal, Caracas");

    @BeforeEach
    void preparar() {
        archivos = new ServicioArchivoJson(carpeta.toString());
        archivos.inicializar();
        MuebleRepositorio muebles = new MuebleRepositorio(archivos);
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

    @Test
    void registraConPreciosCongeladosNumeroYVaciaElCarrito() {
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m1", 2));
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m2", 1));

        PedidoRespuesta pedido = pedidoServicio.registrar("cliente-1", ENTREGA);

        assertEquals("OE-0001", pedido.numero());
        assertEquals(EstadoPedido.PENDIENTE, pedido.estado());
        assertEquals(new BigDecimal("993.33"), pedido.totalUsd());     // 960.00 + 33.33
        assertEquals(new BigDecimal("39733.20"), pedido.totalBs());    // total USD × 40, no suma de Bs.
        assertEquals(new BigDecimal("1333.20"), pedido.items().get(1).precioUnitarioBs());
        assertEquals("Ana", pedido.entrega().nombre());                 // espacios recortados
        assertTrue(pedido.enlaceWhatsapp().startsWith("https://wa.me/584120000000?text="));
        assertTrue(carritoServicio.consultar("cliente-1").items().isEmpty());
    }

    @Test
    void elSegundoRegistroDelMismoCarritoNoDuplica() {
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m1", 1));
        pedidoServicio.registrar("cliente-1", ENTREGA);

        var error = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.registrar("cliente-1", ENTREGA));
        assertEquals(HttpStatus.CONFLICT, error.getEstado());
    }

    @Test
    void numeraConsecutivoEntreClientes() {
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m1", 1));
        carritoServicio.agregarItem("cliente-2", new AgregarItemCarritoSolicitud("m1", 1));
        assertEquals("OE-0001", pedidoServicio.registrar("cliente-1", ENTREGA).numero());
        assertEquals("OE-0002", pedidoServicio.registrar("cliente-2", ENTREGA).numero());
    }

    @Test
    void rechazaStockInsuficienteYNoTocaElCarrito() {
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m2", 1));
        // Otro cambio deja el mueble sin stock después de añadirlo al carrito.
        archivos.modificarLista("muebles.json", Mueble.class, lista -> {
            lista.replaceAll(m -> m.id().equals("m2")
                    ? new Mueble(m.id(), m.codigo(), m.nombre(), m.categoria(), m.descripcion(), m.dimensiones(),
                            m.material(), m.urlFoto(), m.precioUsd(), 0)
                    : m);
            return null;
        });

        var error = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.registrar("cliente-1", ENTREGA));
        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        assertEquals("Stock insuficiente", error.getMessage());
        assertEquals(1, carritoServicio.consultar("cliente-1").items().size());
    }

    @Test
    void preordenSoloParaElDueno() {
        carritoServicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("m1", 1));
        PedidoRespuesta pedido = pedidoServicio.registrar("cliente-1", ENTREGA);

        byte[] pdf = pedidoServicio.generarPreorden("cliente-1", pedido.id()).contenido();
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"));

        var ajeno = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.generarPreorden("cliente-2", pedido.id()));
        assertEquals(HttpStatus.FORBIDDEN, ajeno.getEstado());
        var noExiste = assertThrows(ExcepcionNegocio.class, () -> pedidoServicio.generarPreorden("cliente-1", "x"));
        assertEquals(HttpStatus.NOT_FOUND, noExiste.getEstado());
    }
}
