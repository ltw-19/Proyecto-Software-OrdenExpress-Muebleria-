package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.seguridad.UsuarioAutenticado;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lógica de la épica Gestionar Mueble: Registrar Mueble y Consultar Mueble (Catálogo).
 */
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

        return aRespuesta(guardado, true);
    }

    /**
     * Historia: CONSULTAR MUEBLE / CATÁLOGO (TODOS).
     * Algoritmo:
     *  1. Lee los muebles de muebles.json a través del repositorio.
     *  2. Aplica filtro opcional de categoría exacta (ignorando mayúsculas/minúsculas).
     *  3. Aplica filtro opcional de búsqueda textual insensible a mayúsculas y acentos sobre:
     *     nombre, descripción, material, código y categoría.
     *  4. Transforma cada mueble a MuebleRespuesta:
     *     - Calcula precioBs según la tasa BCV oficial.
     *     - Fija "disponible" según el stock (unidades > 0).
     *     - Solo incluye el número de "unidades" si el usuario tiene rol ADMIN o GESTOR.
     */
    public List<MuebleRespuesta> consultarMuebles(String busqueda, String categoria, UsuarioAutenticado usuario) {
        boolean puedeVerStock = usuario != null && (usuario.rol() == Rol.ADMIN || usuario.rol() == Rol.GESTOR);
        boolean filtrarCategoria = categoria != null && !categoria.isBlank() && !categoria.equalsIgnoreCase("TODAS");
        boolean filtrarBusqueda = busqueda != null && !busqueda.isBlank();
        String termino = filtrarBusqueda ? normalizar(busqueda) : "";

        return muebleRepositorio.listar().stream()
                .filter(mueble -> {
                    if (filtrarCategoria && (mueble.categoria() == null
                            || !mueble.categoria().trim().equalsIgnoreCase(categoria.trim()))) {
                        return false;
                    }
                    if (filtrarBusqueda) {
                        String nombre = normalizar(mueble.nombre());
                        String desc = normalizar(mueble.descripcion());
                        String mat = normalizar(mueble.material());
                        String cod = normalizar(mueble.codigo());
                        String cat = normalizar(mueble.categoria());
                        return nombre.contains(termino)
                                || desc.contains(termino)
                                || mat.contains(termino)
                                || cod.contains(termino)
                                || cat.contains(termino);
                    }
                    return true;
                })
                .map(mueble -> aRespuesta(mueble, puedeVerStock))
                .toList();
    }

    /**
     * Obtiene la lista de nombres únicos de categorías registradas en muebles.json, ordenadas alfabéticamente.
     */
    public List<String> listarCategorias() {
        return muebleRepositorio.listar().stream()
                .map(Mueble::categoria)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private MuebleRespuesta aRespuesta(Mueble mueble, boolean incluirStock) {
        boolean disponible = mueble.unidades() != null && mueble.unidades() > 0;
        Integer unidades = incluirStock ? mueble.unidades() : null;

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
                conversionMoneda.usdABolivares(mueble.precioUsd()),
                disponible,
                unidades);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().toLowerCase(Locale.ROOT);
    }
}
