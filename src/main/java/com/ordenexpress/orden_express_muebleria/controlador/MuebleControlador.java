package com.ordenexpress.orden_express_muebleria.controlador;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.servicio.MuebleServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
