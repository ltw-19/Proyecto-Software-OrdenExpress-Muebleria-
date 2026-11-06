package com.ordenexpress.orden_express_muebleria.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionAlmacenamiento;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Servicio central de persistencia: lee y escribe los archivos planos JSON.
 *
 * Es genérico: sirve para cualquier entidad (Mueble, Usuario, Pedido...).
 * Se inyecta donde se necesite con:
 *
 *     private final ServicioArchivoJson archivos;
 *     public MuebleRepositorio(ServicioArchivoJson archivos) { this.archivos = archivos; }
 *
 * Garantías:
 *  1. Si un archivo no existe, se crea solo con un arreglo vacío "[]".
 *  2. Un error de lectura/escritura (IOException) se registra en el log y se
 *     convierte en ExcepcionAlmacenamiento: el servidor sigue vivo.
 *  3. Dos peticiones simultáneas no corrompen el archivo (candado por archivo).
 *  4. La escritura es "todo o nada": se escribe en un archivo temporal y luego
 *     se reemplaza el original, así nunca queda un JSON a medias.
 *  5. Nunca se guardan elementos null dentro del arreglo ni propiedades null.
 */
@Component
public class ServicioArchivoJson {

    private static final Logger registro = LoggerFactory.getLogger(ServicioArchivoJson.class);

    /** Archivos que el sistema necesita; se crean al arrancar si faltan. */
    private static final List<String> ARCHIVOS_DEL_SISTEMA = List.of(
            "muebles.json", "usuarios.json", "carritos.json", "pedidos.json", "resenas.json");

    private final JsonMapper mapeador;
    private final Path directorioDatos;

    /** Nombres de los archivos que tuvieron que crearse (porque no existían) durante este arranque. */
    private final Set<String> archivosCreadosEnEsteArranque = ConcurrentHashMap.newKeySet();

    /** Un candado de lectura/escritura por cada archivo. */
    private final Map<String, ReentrantReadWriteLock> candados = new ConcurrentHashMap<>();

    /**
     * @param directorio carpeta donde viven los JSON (propiedad app.datos.directorio,
     *                   por defecto "datos", relativa a donde se ejecuta el servidor).
     */
    public ServicioArchivoJson(@Value("${app.datos.directorio:datos}") String directorio) {
        this.directorioDatos = Paths.get(directorio).toAbsolutePath().normalize();

        // Spring Boot 4 usa Jackson 3: el JsonMapper es inmutable y se configura con un "builder".
        // Jackson 3 ya incluye soporte de fechas (Instant, LocalDateTime) y las guarda como
        // texto ISO-8601, así que no hace falta registrar ningún módulo extra.
        this.mapeador = JsonMapper.builder()
                // JSON con sangría para poder revisarlo a mano en la demo.
                .enable(SerializationFeature.INDENT_OUTPUT)
                // Si alguien edita el JSON a mano y agrega un campo extra, no se cae la lectura.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // No escribir propiedades con valor null (evita "basura" en los nodos).
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    /**
     * Se ejecuta una vez al arrancar Spring Boot: crea la carpeta de datos y
     * los archivos que falten, cada uno con "[]".
     *
     * Aquí SÍ se falla rápido: si no se puede ni crear la carpeta, el sistema
     * no puede funcionar y es mejor enterarse al arrancar que en plena demo.
     */
    @PostConstruct
    public void inicializar() {
        try {
            Files.createDirectories(directorioDatos);
        } catch (IOException e) {
            throw falla("No se pudo crear la carpeta de datos: " + directorioDatos, e);
        }
        ARCHIVOS_DEL_SISTEMA.forEach(this::asegurarArchivo);
        registro.info("Carpeta de datos lista en {}", directorioDatos);
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /**
     * Lee TODO el contenido del archivo como lista de objetos.
     * Devuelve una lista nueva y modificable; cambiarla no altera el archivo
     * hasta que se llame a escribirLista o modificarLista.
     *
     * @throws ExcepcionAlmacenamiento si el archivo no se puede leer o su JSON está dañado.
     */
    public <T> List<T> leerLista(String nombreArchivo, Class<T> tipo) {
        asegurarArchivo(nombreArchivo);
        ReentrantReadWriteLock.ReadLock candado = candadoDe(nombreArchivo).readLock();
        candado.lock();
        try {
            return leerSinCandado(nombreArchivo, tipo);
        } finally {
            candado.unlock();
        }
    }

    /**
     * Reemplaza el contenido completo del archivo por la lista recibida.
     * Úsalo solo cuando ya tienes la lista completa; para "agregar/editar/borrar
     * un elemento" usa modificarLista, que es seguro ante peticiones simultáneas.
     */
    public <T> void escribirLista(String nombreArchivo, List<T> lista) {
        asegurarArchivo(nombreArchivo);
        ReentrantReadWriteLock.WriteLock candado = candadoDe(nombreArchivo).writeLock();
        candado.lock();
        try {
            escribirSinCandado(nombreArchivo, lista);
        } finally {
            candado.unlock();
        }
    }

    /**
     * Operación completa "leer - modificar - guardar" bajo un mismo candado.
     * Evita que dos usuarios que guardan a la vez se pisen los cambios.
     *
     * Ejemplo (agregar un mueble):
     *     archivos.modificarLista("muebles.json", Mueble.class, lista -> { lista.add(m); return m; });
     *
     * Si la operación lanza una excepción (por ejemplo, una validación de negocio),
     * el archivo NO se toca.
     *
     * @return lo que devuelva la operación (puede ser null).
     */
    public <T, R> R modificarLista(String nombreArchivo, Class<T> tipo, Function<List<T>, R> operacion) {
        asegurarArchivo(nombreArchivo);
        ReentrantReadWriteLock.WriteLock candado = candadoDe(nombreArchivo).writeLock();
        candado.lock();
        try {
            List<T> lista = leerSinCandado(nombreArchivo, tipo);
            R resultado = operacion.apply(lista);
            escribirSinCandado(nombreArchivo, lista);
            return resultado;
        } finally {
            candado.unlock();
        }
    }

    /** Crea el archivo con "[]" si no existe. Si ya existe, no hace nada. */
    public void asegurarArchivo(String nombreArchivo) {
        Path ruta = resolver(nombreArchivo);
        if (Files.exists(ruta)) {
            return;
        }
        ReentrantReadWriteLock.WriteLock candado = candadoDe(nombreArchivo).writeLock();
        candado.lock();
        try {
            // Se vuelve a comprobar: otra petición pudo crearlo mientras esperábamos el candado.
            if (Files.notExists(ruta)) {
                Files.createDirectories(directorioDatos);
                Files.writeString(ruta, "[]", StandardCharsets.UTF_8);
                archivosCreadosEnEsteArranque.add(nombreArchivo);
                registro.info("Archivo {} no existía: se creó vacío.", nombreArchivo);
            }
        } catch (IOException e) {
            throw falla("No se pudo crear el archivo " + nombreArchivo, e);
        } finally {
            candado.unlock();
        }
    }

    /**
     * Indica si el archivo NO existía y se creó vacío durante este arranque del servidor.
     * Lo usa SemillaServicio para cargar los datos de ejemplo solo la primera vez.
     */
    public boolean fueCreadoEnEsteArranque(String nombreArchivo) {
        return archivosCreadosEnEsteArranque.contains(nombreArchivo);
    }

    // ------------------------------------------------------------------
    // Métodos internos (no toman candado: quien los llama ya lo tiene)
    // ------------------------------------------------------------------

    private <T> List<T> leerSinCandado(String nombreArchivo, Class<T> tipo) {
        Path ruta = resolver(nombreArchivo);
        try {
            // Un archivo vacío (0 bytes) se interpreta como lista vacía.
            if (Files.size(ruta) == 0) {
                return new ArrayList<>();
            }
            JavaType tipoLista = mapeador.getTypeFactory().constructCollectionType(ArrayList.class, tipo);
            List<T> lista = mapeador.readValue(ruta.toFile(), tipoLista);
            if (lista == null) {
                return new ArrayList<>();
            }
            lista.removeIf(Objects::isNull);
            return lista;
        } catch (IOException | JacksonException e) {
            // IMPORTANTE: NO se devuelve una lista vacía aquí. Si el JSON está dañado y
            // devolviéramos [], la siguiente escritura sobrescribiría los datos reales.
            throw falla("No se pudo leer el archivo " + nombreArchivo
                    + " (revisa que el JSON sea válido)", e);
        }
    }

    private void escribirSinCandado(String nombreArchivo, List<?> lista) {
        Path destino = resolver(nombreArchivo);
        Path temporal = resolver(nombreArchivo + ".tmp");

        // Copia sin elementos null, para no dejar valores null dentro del arreglo.
        List<Object> limpia = new ArrayList<>(lista);
        limpia.removeIf(Objects::isNull);

        try {
            // 1) Se escribe completo en un archivo temporal...
            mapeador.writeValue(temporal.toFile(), limpia);
            // 2) ...y solo si salió bien, reemplaza al original.
            try {
                Files.move(temporal, destino,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | JacksonException e) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignorada) {
                // Si ni el temporal se puede borrar, solo queda registrarlo: el original sigue intacto.
                registro.warn("No se pudo borrar el temporal {}", temporal);
            }
            throw falla("No se pudo guardar el archivo " + nombreArchivo, e);
        }
    }

    /** Convierte un nombre de archivo en ruta dentro de la carpeta de datos (y nunca fuera de ella). */
    private Path resolver(String nombreArchivo) {
        Path ruta = directorioDatos.resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(directorioDatos)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido: " + nombreArchivo);
        }
        return ruta;
    }

    private ReentrantReadWriteLock candadoDe(String nombreArchivo) {
        return candados.computeIfAbsent(nombreArchivo, clave -> new ReentrantReadWriteLock());
    }

    /** Registra el error técnico en el log (IOException o JacksonException) y lo envuelve en una excepción controlada. */
    private ExcepcionAlmacenamiento falla(String mensaje, Exception causa) {
        registro.error(mensaje, causa);
        return new ExcepcionAlmacenamiento(mensaje, causa);
    }
}
