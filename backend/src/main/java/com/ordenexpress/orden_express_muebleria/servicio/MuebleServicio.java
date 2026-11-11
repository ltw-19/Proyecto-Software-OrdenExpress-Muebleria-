package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.dto.MuebleRespuesta;
import com.ordenexpress.orden_express_muebleria.dto.MuebleSolicitud;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Lógica de la épica Gestionar Mueble: Registrar Mueble y Consultar Mueble. */
@Service
public class MuebleServicio {

    private static final int MAXIMO_BUSQUEDA = 100;

    private final MuebleRepositorio muebleRepositorio;
    private final ConversionMonedaServicio conversionMoneda;

    public MuebleServicio(MuebleRepositorio muebleRepositorio, ConversionMonedaServicio conversionMoneda) {
        this.muebleRepositorio = muebleRepositorio;
        this.conversionMoneda = conversionMoneda;
    }

    // ------------------------------------------------------------------
    // Registrar Mueble
    // ------------------------------------------------------------------

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

        // Quien registra es ADMIN o GESTOR: puede ver las unidades.
        return aRespuesta(guardado, true);
    }

    // ------------------------------------------------------------------
    // Consultar Mueble
    // ------------------------------------------------------------------

    /**
     * Historia: CONSULTAR MUEBLE (todos los roles). Devuelve el catálogo con precio bimonetario.
     * Algoritmo:
     *  1. Rechaza búsquedas de más de 100 caracteres.
     *  2. Lee todos los muebles de muebles.json.
     *  3. Si viene "categoria", se queda solo con los de esa categoría (sin importar mayúsculas ni tildes).
     *  4. Si viene "busqueda", la separa en palabras y se queda con los muebles que contengan TODAS
     *     las palabras en su nombre, código, material o descripción (sin importar mayúsculas ni tildes).
     *  5. Convierte cada mueble a su respuesta: calcula los bolívares y decide qué mostrar del stock.
     *
     * @param verUnidades true para ADMIN y GESTOR (ven la cifra); false para el CLIENTE (solo ve si hay).
     */
    public List<MuebleRespuesta> consultar(String busqueda, String categoria, boolean verUnidades) {
        if (busqueda != null && busqueda.length() > MAXIMO_BUSQUEDA) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "La búsqueda admite máximo 100 caracteres.");
        }
        String categoriaBuscada = normalizar(categoria);
        List<String> terminos = separarTerminos(busqueda);

        return muebleRepositorio.listar().stream()
                .filter(m -> categoriaBuscada.isEmpty() || normalizar(m.categoria()).equals(categoriaBuscada))
                .filter(m -> coincide(m, terminos))
                .map(m -> aRespuesta(m, verUnidades))
                .toList();
    }

    /**
     * Historia: CONSULTAR MUEBLE (todos los roles). Detalle de un mueble específico por su id.
     * Si no existe → 404.
     *
     * @param verUnidades true para ADMIN y GESTOR (ven la cifra); false para el CLIENTE (solo ve si hay).
     */
    public MuebleRespuesta consultarDetalle(String id, boolean verUnidades) {
        if (id == null || id.isBlank()) {
            throw new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El mueble no existe.");
        }
        Mueble mueble = muebleRepositorio.buscarPorId(id.trim())
                .orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, "El mueble no existe."));
        return aRespuesta(mueble, verUnidades);
    }

    /**
     * Categorías distintas, ordenadas alfabéticamente, para llenar el filtro del catálogo.
     * "Sala" y "sala" cuentan como la misma categoría (se muestra la primera escritura encontrada).
     */
    public List<String> listarCategorias() {
        Map<String, String> unicas = new TreeMap<>();
        for (Mueble mueble : muebleRepositorio.listar()) {
            if (mueble.categoria() != null && !mueble.categoria().isBlank()) {
                unicas.putIfAbsent(normalizar(mueble.categoria()), mueble.categoria().trim());
            }
        }
        return List.copyOf(unicas.values());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static boolean coincide(Mueble mueble, List<String> terminos) {
        if (terminos.isEmpty()) {
            return true;
        }
        String texto = normalizar(valor(mueble.nombre()) + " " + valor(mueble.codigo()) + " "
                + valor(mueble.material()) + " " + valor(mueble.descripcion()));
        return terminos.stream().allMatch(texto::contains);
    }

    private static List<String> separarTerminos(String busqueda) {
        String limpio = normalizar(busqueda);
        return limpio.isEmpty() ? List.of() : Arrays.asList(limpio.split("\\s+"));
    }

    /** Minúsculas, sin tildes y sin espacios sobrantes: "Sofá " y "sofa" se vuelven iguales. */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private MuebleRespuesta aRespuesta(Mueble mueble, boolean verUnidades) {
        boolean disponible = mueble.unidades() != null && mueble.unidades() > 0;
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
                disponible,
                verUnidades ? mueble.unidades() : null);              // el cliente nunca recibe la cifra
    }
}
