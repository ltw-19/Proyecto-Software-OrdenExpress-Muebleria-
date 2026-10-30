package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.IngresoRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.IngresoSolicitud;
import com.ordenexpress.orden_express_muebleria.servicio.CuentaServicio;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de Gestionar Cuenta. Aquí solo vive el ingreso; las demás historias se irán sumando. */
@RestController
@RequestMapping("/api/cuentas")
public class CuentaControlador {

    private final CuentaServicio cuentaServicio;

    public CuentaControlador(CuentaServicio cuentaServicio) {
        this.cuentaServicio = cuentaServicio;
    }

    /** POST /api/cuentas/ingresar → {token, id, nombre, correo, rol}. Es pública. */
    @PostMapping("/ingresar")
    public IngresoRespuesta ingresar(@Valid @RequestBody IngresoSolicitud solicitud) {
        return cuentaServicio.ingresar(solicitud);
    }
}
