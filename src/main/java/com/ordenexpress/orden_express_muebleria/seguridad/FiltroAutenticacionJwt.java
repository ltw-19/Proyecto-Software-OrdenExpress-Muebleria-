package com.ordenexpress.orden_express_muebleria.seguridad;

import com.ordenexpress.orden_express_muebleria.repositorio.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta en CADA petición. Lee la cabecera "Authorization: Bearer <token>",
 * valida el token y, si es válido, deja registrado quién es el usuario y su rol.
 *
 * Si no hay token o es inválido, no registra a nadie y Spring Security responde 401 -> No autorizado.
 *
 * Ojo: NO tiene @Component a propósito. Se crea manualmente en ConfiguracionSeguridad
 * para que Spring Boot no lo registre dos veces.
 */
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;
    private final UsuarioRepositorio usuarioRepositorio;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt, UsuarioRepositorio usuarioRepositorio) {
        this.servicioJwt = servicioJwt;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();

            servicioJwt.validarYObtenerIdUsuario(token)
                    // El rol se toma de usuarios.json (no del token): si el ADMIN cambia un rol,
                    // el cambio se aplica de inmediato y un token viejo no conserva permisos.
                    .flatMap(usuarioRepositorio::buscarPorId)
                    .ifPresent(usuario -> {
                        UsuarioAutenticado principal =
                                new UsuarioAutenticado(usuario.id(), usuario.correo(), usuario.rol());
                        // "ROLE_" es el prefijo que Spring exige para usar hasRole('GESTOR').
                        var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.rol().name());
                        var autenticacion = new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(autoridad));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
        }

        cadena.doFilter(peticion, respuesta);
    }
}
