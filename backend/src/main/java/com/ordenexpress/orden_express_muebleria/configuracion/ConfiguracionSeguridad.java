package com.ordenexpress.orden_express_muebleria.configuracion;

import com.ordenexpress.orden_express_muebleria.repositorio.UsuarioRepositorio;
import com.ordenexpress.orden_express_muebleria.seguridad.FiltroAutenticacionJwt;
import com.ordenexpress.orden_express_muebleria.seguridad.ServicioJwt;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Reglas de seguridad de toda la API:
 *  - Sin sesiones en el servidor (STATELESS): cada petición trae su JWT.
 *  - Solo /api/cuentas/ingresar es pública; todo lo demás exige token válido.
 *  - Los permisos por rol se aplican en cada método con @PreAuthorize("hasRole('GESTOR')").
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http,
                                               ServicioJwt servicioJwt,
                                               UsuarioRepositorio usuarioRepositorio) throws Exception {
        http
                // CSRF protege sesiones con cookies; aquí no hay cookies, se usa token en cabecera.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // pre-vuelo de CORS
                        .requestMatchers("/error").permitAll()
                        // Rutas públicas (solo POST): iniciar sesión y crear cuenta.
                        .requestMatchers(HttpMethod.POST, "/api/cuentas/ingresar", "/api/cuentas/registro").permitAll()
                        .anyRequest().authenticated())
                // Respuestas de error en JSON y en español (mismo formato que ManejadorGlobalExcepciones).
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((peticion, respuesta, ex) -> escribirError(
                                respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                                "Tu sesión no es válida o expiró. Ingresa de nuevo."))
                        .accessDeniedHandler((peticion, respuesta, ex) -> escribirError(
                                respuesta, HttpServletResponse.SC_FORBIDDEN,
                                "No tienes permisos para realizar esta acción.")))
                .addFilterBefore(new FiltroAutenticacionJwt(servicioJwt, usuarioRepositorio),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Codificador de contraseñas: todas las claves se guardan con hash BCrypt. */
    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    /** Permite que el frontend Vue (otro puerto) llame a esta API. */
    @Bean
    public CorsConfigurationSource fuenteCors(@Value("${app.cors.origen}") String origenPermitido) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(origenPermitido));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    private static void escribirError(HttpServletResponse respuesta, int estado, String mensaje)
            throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
