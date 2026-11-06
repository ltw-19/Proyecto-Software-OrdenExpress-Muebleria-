package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.UUID;

/** Lógica de la épica Gestionar Mueble. Por ahora: Registrar Mueble. */
@Service
public class MuebleServicio {

    private final MuebleRepositorio muebleRepositorio;
    private final ConversionMonedaServicio conversionMoneda;

    public MuebleServicio(MuebleRepositorio muebleRepositorio, ConversionMonedaServicio conversionMoneda) {
        this.muebleRepositorio = muebleRepositorio;
        this.conversionMoneda = conversionMoneda;
    }

    /**
     * Historia: REGISTRAR MUEBLE (ADMIN, GESTOR).
     * Algoritmo:
     *  1. Quita los espacios sobrantes de los textos.
     *  2. Deja el precio con exactamente 2 decimales (redondeo HALF_UP).
     *  3. Pide al repositorio que guarde el mueble: él calcula el código siguiente (M-001...)
     *     y arma el mueble con un id UUID, bajo un candado para evitar códigos repetidos.
     *  4. Responde con el mueble guardado y su precio en bolívares calculado con la tasa BCV.
     */
    public MuebleRespuesta registrar(MuebleSolicitud solicitud) {
        var medidas = solicitud.dimensiones();
        var dimensiones = new Mueble.Dimensiones(medidas.ancho(), medidas.alto(), medidas.profundidad());

        Mueble guardado = muebleRepositorio.agregarConCodigoNuevo(codigo -> new Mueble(
                UUID.randomUUID().toString(),
                codigo,
                solicitud.nombre().trim(),
                solicitud.categoria().trim(),
                solicitud.descripcion().trim(),
                dimensiones,
                solicitud.material().trim(),
                solicitud.urlFoto().trim(),
                solicitud.precioUsd().setScale(2, RoundingMode.HALF_UP),
                solicitud.unidades()));

        return aRespuesta(guardado);
    }

    private MuebleRespuesta aRespuesta(Mueble mueble) {
        return new MuebleRespuesta(
                mueble.id(),
                mueble.codigo(),
                mueble.nombre(),
                mueble.categoria(),
                mueble.descripcion(),
                mueble.dimensiones(),
                mueble.material(),
                mueble.urlFoto(),
                mueble.precioUsd(),
                conversionMoneda.usdABolivares(mueble.precioUsd()),   // Bs. calculados, nunca guardados
                mueble.unidades());
    }
}
