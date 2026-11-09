package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.CarritoRespuesta;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.repositorio.CarritoRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Historia "Eliminar ítem del Carrito", probada con archivos JSON reales en una carpeta temporal
 * (sin levantar Spring). Tasa BCV de prueba: 40.00.
 */
class EliminarItemCarritoTest {

    @TempDir
    Path carpeta;

    private ServicioArchivoJson archivos;
    private CarritoServicio servicio;

    @BeforeEach
    void preparar() {
        archivos = new ServicioArchivoJson(carpeta.toString());
        archivos.inicializar();
        servicio = new CarritoServicio(new CarritoRepositorio(archivos), new MuebleRepositorio(archivos),
                new ConversionMonedaServicio(new BigDecimal("40.00")));

        archivos.escribirLista("muebles.json", List.of(
                mueble("m1", "M-001", "480.00", 8),
                mueble("m2", "M-002", "19.99", 5)));
        archivos.escribirLista("carritos.json", List.of(new Carrito("cliente-1", List.of(
                new Carrito.ItemCarrito("m1", 2, Instant.now()),
                new Carrito.ItemCarrito("m2", 3, Instant.now())), Instant.now())));
    }

    @Test
    void quitaElItemYRecalculaLosTotales() {
        CarritoRespuesta respuesta = servicio.eliminarItem("cliente-1", "m1");

        assertEquals(1, respuesta.cantidadItems());
        assertEquals("m2", respuesta.items().get(0).idMueble());
        assertEquals(new BigDecimal("59.97"), respuesta.totalUsd());
        assertEquals(new BigDecimal("2398.80"), respuesta.totalBs());   // 59.97 × 40
        // Lo guardado coincide con lo respondido.
        assertEquals(1, servicio.consultar("cliente-1").cantidadItems());
    }

    @Test
    void alQuitarElUltimoItemQuedaVacioSinNullEnElArchivo() throws Exception {
        servicio.eliminarItem("cliente-1", "m1");
        CarritoRespuesta respuesta = servicio.eliminarItem("cliente-1", "m2");

        assertTrue(respuesta.items().isEmpty());
        assertEquals(new BigDecimal("0.00"), respuesta.totalUsd());
        assertEquals(new BigDecimal("0.00"), respuesta.totalBs());
        String json = Files.readString(carpeta.resolve("carritos.json"));
        assertTrue(json.contains("\"items\" : [ ]") || json.contains("\"items\":[]"), json);
        assertFalse(json.contains("null"), json);
    }

    @Test
    void elDobleClicRecibe404YNoCambiaNada() {
        servicio.eliminarItem("cliente-1", "m1");

        var error = assertThrows(ExcepcionNegocio.class, () -> servicio.eliminarItem("cliente-1", "m1"));
        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("Este mueble no está en tu carrito.", error.getMessage());
        assertEquals(1, servicio.consultar("cliente-1").cantidadItems());
    }

    @Test
    void clienteSinCarritoRecibe404YNoSeLeCreaUno() {
        var error = assertThrows(ExcepcionNegocio.class, () -> servicio.eliminarItem("cliente-nuevo", "m1"));
        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals(1, archivos.leerLista("carritos.json", Carrito.class).size());
    }

    @Test
    void soloTocaElCarritoDelUsuarioDelToken() {
        archivos.escribirLista("carritos.json", List.of(
                new Carrito("otro-cliente", List.of(new Carrito.ItemCarrito("m1", 1, Instant.now())), Instant.now()),
                new Carrito("cliente-1", List.of(new Carrito.ItemCarrito("m2", 1, Instant.now())), Instant.now())));

        // cliente-1 no tiene m1: no puede borrarlo del carrito ajeno.
        assertThrows(ExcepcionNegocio.class, () -> servicio.eliminarItem("cliente-1", "m1"));
        assertEquals(1, servicio.consultar("otro-cliente").cantidadItems());
    }

    @Test
    void sePuedeQuitarUnMuebleQueYaNoExisteEnElCatalogo() {
        archivos.escribirLista("carritos.json", List.of(new Carrito("cliente-1", Arrays.asList(
                new Carrito.ItemCarrito("m1", 1, Instant.now()),
                null,
                new Carrito.ItemCarrito("borrado", 1, Instant.now())), Instant.now())));

        CarritoRespuesta respuesta = servicio.eliminarItem("cliente-1", "borrado");

        assertEquals(1, respuesta.cantidadItems());
        List<Carrito.ItemCarrito> guardados =
                archivos.leerLista("carritos.json", Carrito.class).get(0).items();
        assertEquals(1, guardados.size());               // también se limpió el ítem null
        assertNotNull(guardados.get(0));
    }

    @Test
    void noModificaElStock() {
        servicio.eliminarItem("cliente-1", "m1");

        Mueble m1 = archivos.leerLista("muebles.json", Mueble.class).get(0);
        assertEquals(8, m1.unidades());
    }

    @Test
    void rechazaMuebleVacio() {
        var error = assertThrows(ExcepcionNegocio.class, () -> servicio.eliminarItem("cliente-1", "  "));
        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
    }

    private static Mueble mueble(String id, String codigo, String precio, int unidades) {
        return new Mueble(id, codigo, "Mueble " + codigo, "Sala", "Descripción", null, "Madera",
                "https://ejemplo.com/foto.png", new BigDecimal(precio), unidades);
    }
}
