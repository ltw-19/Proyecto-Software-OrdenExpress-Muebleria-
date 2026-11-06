package com.ordenexpress.orden_express_muebleria.excepcion;

/**
 * Se lanza cuando falla la lectura o escritura de un archivo JSON.
 *
 * Es una excepción "no verificada" (RuntimeException) para no obligar a cada
 * Service/Controller a declarar "throws IOException". Más adelante un
 * manejador global (@RestControllerAdvice) la convertirá en una respuesta
 * HTTP 500 con un mensaje claro en español, sin tumbar el servidor.
 */
public class ExcepcionAlmacenamiento extends RuntimeException {

    public ExcepcionAlmacenamiento(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
