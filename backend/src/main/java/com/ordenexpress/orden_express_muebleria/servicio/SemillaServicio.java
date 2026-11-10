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
 *    y muebles.json está vacío, carga 6 muebles de ejemplo con fotos reales de alta calidad.
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
                entorno.getRequiredProperty(base + "apellido"),
                entorno.getRequiredProperty(base + "correo").trim().toLowerCase(Locale.ROOT),
                codificador.encode(entorno.getRequiredProperty(base + "clave")), // nunca texto plano
                null,
                null,
                rol,
                Instant.now());
    }

    private void sembrarMuebles(boolean sistemaNuevo) {
        if (!sistemaNuevo || !muebleRepositorio.listar().isEmpty()) {
            return;
        }
        // Los mismos 6 muebles de los mockups (inventario). Medidas en cm: ancho, alto, profundidad.
        muebleRepositorio.guardarTodos(List.of(
                mueble("M-001", "Sofá Alba", "Sala",
                        "Sofá de tres puestos tapizado en lino beige, con estructura y patas de madera de nogal. "
                                + "Una pieza cómoda y atemporal para tu sala.",
                        "210", "82", "90", "Lino beige y madera de nogal",
                        "https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?auto=format&fit=crop&w=800&q=80",
                        "480.00", 8),
                mueble("M-002", "Butaca Nido", "Sala",
                        "Butaca de brazos en madera de nogal con cojines de lino claro. Ideal para un rincón de lectura.",
                        "78", "84", "80", "Madera de nogal y lino",
                        "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80",
                        "180.00", 12),
                mueble("M-003", "Mesa de centro Olmo", "Sala",
                        "Mesa de centro baja de líneas suaves, con patas redondeadas y acabado cálido.",
                        "110", "40", "60", "Madera de olmo",
                        "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?auto=format&fit=crop&w=800&q=80",
                        "220.00", 6),
                mueble("M-004", "Cama Serena", "Dormitorio",
                        "Cama con cabecero tapizado en lino y base de madera. Una pieza sobria para un dormitorio tranquilo.",
                        "160", "100", "200", "Madera de nogal y lino",
                        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80",
                        "650.00", 4),
                mueble("M-005", "Aparador Roble", "Comedor",
                        "Aparador de tres puertas en roble, con patas inclinadas y mucho espacio de guardado.",
                        "160", "75", "42", "Madera de roble",
                        "https://images.unsplash.com/photo-1595428774223-ef52624120d2?auto=format&fit=crop&w=800&q=80",
                        "390.00", 5),
                mueble("M-006", "Mesa auxiliar Lía", "Sala",
                        "Mesa auxiliar redonda de base cilíndrica, perfecta junto al sofá o la cama.",
                        "45", "52", "45", "Madera de nogal",
                        "https://images.unsplash.com/photo-1530018607912-eff2daa1bac4?auto=format&fit=crop&w=800&q=80",
                        "95.00", 10)));
        registro.info("Sistema nuevo: se cargaron 6 muebles de ejemplo con fotos reales en muebles.json.");
    }

    private Mueble mueble(String codigo, String nombre, String categoria, String descripcion,
                          String ancho, String alto, String profundidad,
                          String material, String urlFoto, String precioUsd, int unidades) {
        return new Mueble(UUID.randomUUID().toString(), codigo, nombre, categoria, descripcion,
                new Mueble.Dimensiones(new BigDecimal(ancho), new BigDecimal(alto), new BigDecimal(profundidad)),
                material, urlFoto, new BigDecimal(precioUsd), unidades);
    }
}