package com.ordenexpress.orden_express_muebleria.util;

import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Lee una "cantidad" del JSON exigiendo el FORMATO correcto: un número entero escrito como número.
 *
 * Sin esto, Jackson "adivina" y acepta valores que no son enteros:
 *   1.5  → lo recorta a 1 en silencio      "2" → lo convierte a 2
 *   2.0  → lo acepta como 2                1e2 → lo convierte a 100
 * Con este deserializador todos esos casos responden 400 con un mensaje claro.
 *
 * El VALOR (positivo, no nulo, máximo) lo siguen validando @NotNull/@Min/@Max del DTO
 * y, por seguridad, también el servicio. Un null llega como null y lo rechaza @NotNull.
 *
 * Uso en un record DTO:  @JsonDeserialize(using = DeserializadorCantidadEntera.class) Integer cantidad
 */
public class DeserializadorCantidadEntera extends ValueDeserializer<Integer> {

    private static final String MENSAJE_FORMATO = "La cantidad debe ser un número entero, sin decimales ni comillas.";

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext contexto) {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, MENSAJE_FORMATO);
        }
        // Enteros que ni caben en un int (por ejemplo 99999999999): fuera de rango.
        if (parser.getNumberType() != JsonParser.NumberType.INT) {
            throw new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 100000.");
        }
        return parser.getIntValue();
    }
}
