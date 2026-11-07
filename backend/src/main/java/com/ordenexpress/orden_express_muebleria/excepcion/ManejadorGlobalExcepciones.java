package com.ordenexpress.orden_express_muebleria.excepcion;

import com.ordenexpress.orden_express_muebleria.dto.RespuestaError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Atrapa las excepciones de todos los controladores y las convierte en una
 * respuesta JSON uniforme: {"mensaje": "..."} en español.
 * Así Vue siempre sabe dónde leer el texto del error para mostrarlo en un modal.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    /** Errores de negocio y seguridad con su propio código HTTP (401, 403, 404, 409...). */
    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<RespuestaError> manejarNegocio(ExcepcionNegocio e) {
        return ResponseEntity.status(e.getEstado()).body(new RespuestaError(e.getMessage()));
    }

    /** Campos inválidos según las anotaciones (@NotBlank, @DecimalMin...): HTTP 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return ResponseEntity.badRequest().body(new RespuestaError(mensaje));
    }

    /**
     * JSON mal formado o con tipos incorrectos: HTTP 400.
     * Si el error lo lanzó un deserializador propio (por ejemplo DeserializadorCantidadEntera)
     * con una ExcepcionNegocio, se responde con SU mensaje, que es más claro que el genérico.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarCuerpoInvalido(HttpMessageNotReadableException e) {
        for (Throwable causa = e.getCause(); causa != null && causa != causa.getCause(); causa = causa.getCause()) {
            if (causa instanceof ExcepcionNegocio negocio) {
                return manejarNegocio(negocio);
            }
        }
        return ResponseEntity.badRequest()
                .body(new RespuestaError("Los datos enviados no tienen un formato válido."));
    }

    /** Falla al leer o escribir los archivos JSON: HTTP 500 (el detalle técnico ya quedó en el log). */
    @ExceptionHandler(ExcepcionAlmacenamiento.class)
    public ResponseEntity<RespuestaError> manejarAlmacenamiento(ExcepcionAlmacenamiento e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RespuestaError("No se pudo acceder al almacenamiento de datos. Intenta de nuevo."));
    }
}
