package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import com.ordenexpress.orden_express_muebleria.servicio.MuebleServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de Gestionar Mueble. Las demás historias de la épica se irán sumando aquí. */
@RestController
@RequestMapping("/api/muebles")
public class MuebleControlador {

    private final MuebleServicio muebleServicio;

    public MuebleControlador(MuebleServicio muebleServicio) {
        this.muebleServicio = muebleServicio;
    }

    /** POST /api/muebles → 201 con el mueble registrado. Solo ADMIN y GESTOR. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<MuebleRespuesta> registrar(@Valid @RequestBody MuebleSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(muebleServicio.registrar(solicitud));
    }

    /**
     * GET /api/muebles?busqueda=...&categoria=... → catálogo. Cualquier usuario con sesión.
     * Los dos filtros son opcionales y se pueden combinar. ADMIN y GESTOR reciben además la cifra
     * de unidades; el CLIENTE solo recibe "disponible".
     */
    @GetMapping
    public List<MuebleRespuesta> consultar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @RequestParam(required = false) String busqueda,
                                           @RequestParam(required = false) String categoria) {
        boolean verUnidades = usuario.rol() != Rol.CLIENTE;
        return muebleServicio.consultar(busqueda, categoria, verUnidades);
    }

    /** GET /api/muebles/categorias → lista de categorías para el filtro. Cualquier usuario con sesión. */
    @GetMapping("/categorias")
    public List<String> categorias() {
        return muebleServicio.listarCategorias();
    }
}
