package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.CarritoRespuesta;
import com.ordenexpress.orden_express_muebleria.modelo.Carrito;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.repositorio.CarritoRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.util.ServicioArchivoJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Historia "Consultar Carrito", probada con archivos JSON reales en una carpeta temporal
 * (sin levantar Spring). Tasa BCV de prueba: 40.00.
 */
class ConsultarCarritoTest {

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
                mueble("m2", "M-002", "19.99", 0)));   // agotado después de añadirlo
    }

    @Test
    void clienteSinCarritoRecibeCarritoVacioYNoSeCreaNadaEnElArchivo() throws Exception {
        CarritoRespuesta respuesta = servicio.consultar("cliente-nuevo");

        assertTrue(respuesta.items().isEmpty());
        assertEquals(0, respuesta.cantidadItems());
        assertEquals(new BigDecimal("0.00"), respuesta.totalUsd());
        assertEquals(new BigDecimal("0.00"), respuesta.totalBs());
        assertEquals("[]", Files.readString(carpeta.resolve("carritos.json")).trim());
    }

    @Test
    void calculaSubtotalesYTotalesConLosPreciosActuales() {
        archivos.escribirLista("carritos.json", List.of(new Carrito("cliente-1", List.of(
                new Carrito.ItemCarrito("m1", 2, Instant.now()),
                new Carrito.ItemCarrito("m2", 3, Instant.now())), Instant.now())));

        CarritoRespuesta respuesta = servicio.consultar("cliente-1");

        assertEquals(2, respuesta.cantidadItems());
        assertEquals(new BigDecimal("960.00"), respuesta.items().get(0).subtotalUsd());
        assertEquals(new BigDecimal("59.97"), respuesta.items().get(1).subtotalUsd());
        assertEquals(new BigDecimal("1019.97"), respuesta.totalUsd());
        assertEquals(new BigDecimal("40798.80"), respuesta.totalBs());   // 1019.97 × 40, no suma de Bs.
        assertTrue(respuesta.items().get(0).disponible());
        assertFalse(respuesta.items().get(1).disponible());              // agotado: se avisa, sin cifra
    }

    @Test
    void soloDevuelveElCarritoDelUsuarioDelToken() {
        archivos.escribirLista("carritos.json", List.of(
                new Carrito("otro-cliente", List.of(new Carrito.ItemCarrito("m1", 1, Instant.now())), Instant.now())));

        assertTrue(servicio.consultar("cliente-1").items().isEmpty());
    }

    @Test
    void ignoraItemsDanadosOMueblesQueYaNoExisten() {
        archivos.escribirLista("carritos.json", List.of(new Carrito("cliente-1", Arrays.asList(
                new Carrito.ItemCarrito("m1", 1, Instant.now()),
                new Carrito.ItemCarrito("no-existe", 1, Instant.now()),
                new Carrito.ItemCarrito("m2", null, Instant.now()),
                new Carrito.ItemCarrito("m2", -4, Instant.now())), Instant.now())));

        CarritoRespuesta respuesta = servicio.consultar("cliente-1");

        assertEquals(1, respuesta.cantidadItems());
        assertEquals(new BigDecimal("480.00"), respuesta.totalUsd());
    }

    private static Mueble mueble(String id, String codigo, String precio, int unidades) {
        return new Mueble(id, codigo, "Mueble " + codigo, "Sala", "Descripción", null, "Madera",
                "https://ejemplo.com/foto.png", new BigDecimal(precio), unidades);
    }
}
