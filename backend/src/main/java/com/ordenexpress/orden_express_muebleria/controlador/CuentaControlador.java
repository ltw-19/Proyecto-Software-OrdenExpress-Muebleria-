package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.CambioRolSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.CuentaRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.ModificacionCuentaSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.RegistroSolicitud;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import com.ordenexpress.orden_express_muebleria.servicio.CuentaServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de Gestionar Cuenta. Las demás historias de la épica se irán sumando aquí. */
@RestController
@RequestMapping("/api/cuentas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CuentaControlador {

    private final CuentaServicio cuentaServicio;

    public CuentaControlador(CuentaServicio cuentaServicio) {
        this.cuentaServicio = cuentaServicio;
    }

    /** POST /api/cuentas/ingresar → {token, id, nombre, apellido, correo, telefono, direccion, rol}. Pública. */
    @PostMapping("/ingresar")
    public IngresoRespuesta ingresar(@Valid @RequestBody IngresoSolicitud solicitud) {
        return cuentaServicio.ingresar(solicitud);
    }

    /** POST /api/cuentas/registro → 201 con la cuenta creada (rol CLIENTE). Pública. */
    @PostMapping("/registro")
    public ResponseEntity<CuentaRespuesta> registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaServicio.crearCuenta(solicitud));
    }

    /**
     * PUT /api/cuentas/yo → el usuario edita SUS datos personales. Solo ADMIN y CLIENTE
     * (el Gestor de Ventas no tiene esta historia en el backlog).
     * El id viene del token, no del cuerpo ni de la URL.
     */
    @PutMapping("/yo")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public CuentaRespuesta modificarMisDatos(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody ModificacionCuentaSolicitud solicitud) {
        return cuentaServicio.modificarMisDatos(usuario.id(), solicitud);
    }

    /** GET /api/cuentas → lista de cuentas para gestionar roles. Solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CuentaRespuesta> listar() {
        return cuentaServicio.listarCuentas();
    }

    /** PUT /api/cuentas/{id}/rol → el ADMIN cambia el rol de otra cuenta. */
    @PutMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public CuentaRespuesta cambiarRol(@AuthenticationPrincipal UsuarioAutenticado admin,
                                      @PathVariable String id,
                                      @Valid @RequestBody CambioRolSolicitud solicitud) {
        return cuentaServicio.cambiarRol(admin.id(), id, solicitud.rol());
    }
}
