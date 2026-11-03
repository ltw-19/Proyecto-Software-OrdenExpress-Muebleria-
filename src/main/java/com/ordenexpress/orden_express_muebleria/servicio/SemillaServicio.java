package com.ordenexpress.orden_express_muebleria.servicio;

import com.ordenexpress.orden_express_muebleria.modelo.Mueble;
import com.ordenexpress.orden_express_muebleria.modelo.Rol;
import com.ordenexpress.orden_express_muebleria.modelo.Usuario;
import com.ordenexpress.orden_express_muebleria.repositorio.MuebleRepositorio;
import com.ordenexpress.orden_express_muebleria.repositorio.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * REQUISITO TÉCNICO DE INICIALIZACIÓN (no es una historia de usuario).
 * Al arrancar el backend deja el sistema listo para usarse, aunque la carpeta
 * datos/ no exista (porque está en el .gitignore):
 *  - Usuarios: si usuarios.json está vacío, crea el ADMIN y el Gestor de Ventas iniciales.
 *  - Muebles: si es un sistema nuevo (en este arranque se crearon los usuarios iniciales)
 *    y muebles.json está vacío, carga 6 muebles de ejemplo.
 */
@Component
public class SemillaServicio implements ApplicationRunner {

    private static final Logger registro = LoggerFactory.getLogger(SemillaServicio.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final MuebleRepositorio muebleRepositorio;
    private final PasswordEncoder codificador;
    private final Environment entorno;

    public SemillaServicio(UsuarioRepositorio usuarioRepositorio, MuebleRepositorio muebleRepositorio,
                           PasswordEncoder codificador, Environment entorno) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.muebleRepositorio = muebleRepositorio;
        this.codificador = codificador;
        this.entorno = entorno;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        boolean sistemaNuevo = sembrarUsuarios();
        sembrarMuebles(sistemaNuevo);
    }

    /** @return true si el sistema era nuevo y se crearon los usuarios iniciales. */
    private boolean sembrarUsuarios() {
        if (!usuarioRepositorio.listar().isEmpty()) {
            return false;
        }
        Usuario admin = crearUsuarioSemilla("admin", Rol.ADMIN);
        Usuario gestor = crearUsuarioSemilla("gestor", Rol.GESTOR);
        usuarioRepositorio.guardarTodos(List.of(admin, gestor));
        registro.warn("usuarios.json estaba vacío: se crearon el ADMIN ({}) y el GESTOR ({}) iniciales. "
                + "Cambia sus claves antes de la entrega.", admin.correo(), gestor.correo());
        return true;
    }

    /** Lee nombre, correo y clave desde application.properties (app.semilla.<prefijo>.*). */
    private Usuario crearUsuarioSemilla(String prefijo, Rol rol) {
        String base = "app.semilla." + prefijo + ".";
        return new Usuario(
                UUID.randomUUID().toString(),
                entorno.getRequiredProperty(base + "nombre"),
                entorno.getRequiredProperty(base + "correo").trim().toLowerCase(Locale.ROOT),
                codificador.encode(entorno.getRequiredProperty(base + "clave")), // nunca texto plano
                null,
                null,
                rol,
                Instant.now());
    }

    private void sembrarMuebles(boolean sistemaNuevo) {
        // Solo en un sistema nuevo: así, si más adelante se eliminan todos los muebles, no reaparecen
        // los de ejemplo al reiniciar. Tampoco depende de que muebles.json ya existiera vacío por un
        // intento de arranque anterior que falló.
        if (!sistemaNuevo || !muebleRepositorio.listar().isEmpty()) {
            return;
        }
        muebleRepositorio.guardarTodos(List.of(
                mueble("Sofá Milán 3 puestos", "Sala", "210", "85", "95",
                        "Madera de pino y tela antimanchas", "480.00"),
                mueble("Mesa de centro Nogal", "Sala", "110", "45", "60",
                        "Madera de nogal", "135.00"),
                mueble("Comedor Roma 6 puestos", "Comedor", "180", "78", "90",
                        "Roble macizo", "620.00"),
                mueble("Cama Queen Aurora", "Dormitorio", "160", "110", "210",
                        "MDF enchapado y madera", "390.00"),
                mueble("Escritorio Ejecutivo Lima", "Oficina", "140", "75", "70",
                        "Melamina y estructura metálica", "210.00"),
                mueble("Biblioteca Cedro 5 niveles", "Oficina", "90", "180", "35",
                        "Madera de cedro", "175.00")));
        registro.info("Sistema nuevo: se cargaron 6 muebles de ejemplo en muebles.json.");
    }

    private Mueble mueble(String nombre, String categoria, String ancho, String alto, String profundidad,
                          String material, String precioUsd) {
        // Foto de relleno con los colores de la marca. Reemplázalas por fotos reales cuando las tengan.
        String texto = URLEncoder.encode(sinTildes(nombre), StandardCharsets.UTF_8);
        String urlFoto = "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=" + texto;
        return new Mueble(UUID.randomUUID().toString(), nombre, categoria,
                new Mueble.Dimensiones(new BigDecimal(ancho), new BigDecimal(alto), new BigDecimal(profundidad)),
                material, urlFoto, new BigDecimal(precioUsd));
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
