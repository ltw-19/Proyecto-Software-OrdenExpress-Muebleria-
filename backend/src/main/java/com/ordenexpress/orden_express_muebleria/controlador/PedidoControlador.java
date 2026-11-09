package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.PedidoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.RegistrarPedidoSolicitud;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import com.ordenexpress.orden_express_muebleria.servicio.PedidoServicio;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de Gestionar Pedido. Las demás historias de la épica se irán sumando aquí. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoControlador {

    private final PedidoServicio pedidoServicio;

    public PedidoControlador(PedidoServicio pedidoServicio) {
        this.pedidoServicio = pedidoServicio;
    }

    /**
     * POST /api/pedidos → 201 con el pedido registrado y su enlace de WhatsApp. Solo CLIENTE.
     * El cuerpo solo trae los datos de entrega: los ítems salen del carrito del cliente del token.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoRespuesta> registrar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @Valid @RequestBody RegistrarPedidoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoServicio.registrar(usuario.id(), solicitud));
    }

    /**
     * GET /api/pedidos → 200 con la lista de pedidos, del más reciente al más antiguo. TODOS los roles.
     * CLIENTE: solo los suyos. ADMIN y GESTOR: bandeja global (todos).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'CLIENTE')")
    public List<PedidoRespuesta> consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pedidoServicio.consultar(usuario.id(), usuario.rol());
    }

    /**
     * GET /api/pedidos/{id} → 200 con el detalle del pedido. TODOS los roles.
     * El CLIENTE solo puede ver los suyos (403); no existe → 404.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'CLIENTE')")
    public PedidoRespuesta consultarDetalle(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @PathVariable String id) {
        return pedidoServicio.consultarDetalle(usuario.id(), usuario.rol(), id);
    }

    /**
     * PUT /api/pedidos/{id}/aprobar → 200 con el pedido APROBADO. Solo GESTOR (el ADMIN solo consulta).
     * Descuenta el stock de todos los ítems o de ninguno. 409 si no está PENDIENTE o no hay stock.
     */
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('GESTOR')")
    public PedidoRespuesta aprobar(@PathVariable String id) {
        return pedidoServicio.aprobar(id);
    }

    /**
     * PUT /api/pedidos/{id}/rechazar → 200 con el pedido RECHAZADO. Solo GESTOR.
     * No toca el stock. 409 si no está PENDIENTE.
     */
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('GESTOR')")
    public PedidoRespuesta rechazar(@PathVariable String id) {
        return pedidoServicio.rechazar(id);
    }

    /**
     * DELETE /api/pedidos/{id} → 204 sin cuerpo. CLIENTE (solo los suyos y en PENDIENTE) y GESTOR (cualquiera).
     * El ADMIN no puede eliminar (403). No devuelve stock.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'GESTOR')")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @PathVariable String id) {
        pedidoServicio.eliminar(usuario.id(), usuario.rol(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/pedidos/{id}/preorden → 200 con la Pre-orden en PDF (descarga). Solo el CLIENTE dueño.
     * Descargarla no registra ni duplica nada: el pedido ya existe.
     */
    @GetMapping("/{id}/preorden")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<byte[]> descargarPreorden(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                    @PathVariable String id) {
        PedidoServicio.Preorden preorden = pedidoServicio.generarPreorden(usuario.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("preorden-" + preorden.numero() + ".pdf").build().toString())
                .body(preorden.contenido());
    }
}
