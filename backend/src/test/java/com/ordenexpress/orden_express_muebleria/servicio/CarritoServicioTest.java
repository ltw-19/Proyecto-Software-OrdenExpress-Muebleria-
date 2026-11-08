package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * "Verificar Datos" de Añadir ítem al Carrito, probado directamente en el servicio
 * (sin pasar por @Valid del controlador).
 *
 * Los repositorios se pasan como null a propósito: si la validación fallara y el servicio
 * intentara consultar el stock, la prueba explotaría con NullPointerException en vez de 400.
 */
class CarritoServicioTest {

    private final CarritoServicio servicio = new CarritoServicio(null, null, null);

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, 100001})
    void rechazaCantidadFueraDeRangoAntesDeRevisarElStock(int cantidad) {
        var error = assertThrows(ExcepcionNegocio.class,
                () -> servicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("M-1", cantidad)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
    }

    @Test
    void rechazaCantidadNula() {
        var error = assertThrows(ExcepcionNegocio.class,
                () -> servicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("M-1", null)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("La cantidad es obligatoria.", error.getMessage());
    }

    @Test
    void rechazaMuebleVacio() {
        var error = assertThrows(ExcepcionNegocio.class,
                () -> servicio.agregarItem("cliente-1", new AgregarItemCarritoSolicitud("  ", 1)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
    }
}
