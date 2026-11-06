package com.ordenexpress.orden_express_muebleria.seguridad;

import com.ordenexpress.orden_express_muebleria.modelo.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Crea y valida los tokens JWT firmados con la clave secreta del servidor. */
@Service
public class ServicioJwt {

    private final SecretKey clave;
    private final Duration duracion;

    public ServicioJwt(@Value("${app.jwt.secreto}") String secreto,
                       @Value("${app.jwt.horas-expiracion}") long horasExpiracion) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        // HS256 exige una clave de al menos 256 bits (32 bytes).
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secreto debe tener al menos 32 caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofHours(horasExpiracion);
    }

    /**
     * Genera el token del usuario. El id va en el campo estándar "subject".
     * El rol se incluye solo como información: el servidor NO lo usa para decidir
     * permisos (siempre relee el rol real desde usuarios.json).
     */
    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.id())
                .claim("rol", usuario.rol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /**
     * Si el token es auténtico y no ha vencido, devuelve el id del usuario.
     * Si está alterado, vencido o mal formado, devuelve vacío (nunca lanza error).
     */
    public Optional<String> validarYObtenerIdUsuario(String token) {
        try {
            String id = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(id);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
