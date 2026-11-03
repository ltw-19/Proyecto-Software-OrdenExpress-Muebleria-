package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.CuentaRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.dto.RegistroSolicitud;
import com.ordenexpress.orden_express_muebleria.servicio.CuentaServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de Gestionar Cuenta. Las demás historias de la épica se irán sumando aquí. */
@RestController
@RequestMapping("/api/cuentas")
public class CuentaControlador {

    private final CuentaServicio cuentaServicio;

    public CuentaControlador(CuentaServicio cuentaServicio) {
        this.cuentaServicio = cuentaServicio;
    }

    /** POST /api/cuentas/ingresar → {token, id, nombre, correo, rol}. Pública. */
    @PostMapping("/ingresar")
    public IngresoRespuesta ingresar(@Valid @RequestBody IngresoSolicitud solicitud) {
        return cuentaServicio.ingresar(solicitud);
    }

    /** POST /api/cuentas/registro → 201 con la cuenta creada (rol CLIENTE). Pública. */
    @PostMapping("/registro")
    public ResponseEntity<CuentaRespuesta> registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaServicio.crearCuenta(solicitud));
    }
}
