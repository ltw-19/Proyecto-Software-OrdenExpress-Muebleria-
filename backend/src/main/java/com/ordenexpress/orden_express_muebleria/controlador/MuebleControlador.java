package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import com.ordenexpress.orden_express_muebleria.servicio.MuebleServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de Gestionar Mueble: Registrar Mueble y Consultar Catálogo / Categorías.
 */
@RestController
@RequestMapping("/api/muebles")
@CrossOrigin(origins = "*", allowedHeaders = "*")
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
     * GET /api/muebles?busqueda=&categoria= → lista de muebles según filtros opcionales.
     * Accesible a todos los usuarios autenticados. La cifra de stock en unidades solo se revela a ADMIN y GESTOR.
     */
    @GetMapping
    public List<MuebleRespuesta> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String categoria,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return muebleServicio.consultarMuebles(busqueda, categoria, usuario);
    }

    /**
     * GET /api/muebles/categorias → lista de nombres de categorías ordenadas para el filtro del catálogo.
     */
    @GetMapping("/categorias")
    public List<String> listarCategorias() {
        return muebleServicio.listarCategorias();
    }
}
