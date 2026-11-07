package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.AgregarItemCarritoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.CarritoRespuesta;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import com.ordenexpress.orden_express_muebleria.servicio.CarritoServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de Gestionar Carrito. Las demás historias de la épica se irán sumando aquí. */
@RestController
@RequestMapping("/api/carrito")
public class CarritoControlador {

    private final CarritoServicio carritoServicio;

    public CarritoControlador(CarritoServicio carritoServicio) {
        this.carritoServicio = carritoServicio;
    }

    /**
     * GET /api/carrito → 200 con el carrito del cliente (vacío si aún no añadió nada). Solo CLIENTE.
     * El carrito que se devuelve es SIEMPRE el del token: no hay forma de pedir el de otra persona.
     */
    @GetMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public CarritoRespuesta consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return carritoServicio.consultar(usuario.id());
    }

    /**
     * POST /api/carrito/items → 201 con el carrito actualizado. Solo CLIENTE.
     * El dueño del carrito sale del token, nunca del cuerpo ni de la URL.
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CarritoRespuesta> agregarItem(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                        @Valid @RequestBody AgregarItemCarritoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoServicio.agregarItem(usuario.id(), solicitud));
    }
}
