package com.ordenexpress.orden_express_muebleria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /api/muebles (historia "Registrar Mueble").
 *
 * No incluye "id" ni "codigo": los genera el servidor.
 * No incluye precio en bolívares: se calcula con la tasa BCV, nunca se acepta del navegador.
 */
public record MuebleSolicitud(
        @NotBlank(message = "El nombre del mueble es obligatorio.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.") String nombre,

        @NotBlank(message = "La categoría es obligatoria.")
        @Size(max = 50, message = "La categoría admite máximo 50 caracteres.") String categoria,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 500, message = "La descripción admite máximo 500 caracteres.") String descripcion,

        @NotNull(message = "Las dimensiones son obligatorias.") @Valid DimensionesSolicitud dimensiones,

        @NotBlank(message = "El material es obligatorio.")
        @Size(max = 100, message = "El material admite máximo 100 caracteres.") String material,

        // Solo http:// o https://. Esto también impide direcciones peligrosas como "javascript:...".
        @NotBlank(message = "La URL de la foto es obligatoria.")
        @Size(max = 500, message = "La URL de la foto admite máximo 500 caracteres.")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "La URL de la foto debe comenzar con http:// o https://.")
        String urlFoto,

        @NotNull(message = "El precio en USD es obligatorio.")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero.")
        @Digits(integer = 7, fraction = 2, message = "El precio admite máximo 2 decimales y 7 enteros.")
        BigDecimal precioUsd,

        @NotNull(message = "Las unidades disponibles son obligatorias.")
        @Min(value = 0, message = "Las unidades disponibles no pueden ser negativas.")
        @Max(value = 100000, message = "Las unidades disponibles admiten máximo 100000.")
        Integer unidades) {

    /** Dimensiones en centímetros. */
    public record DimensionesSolicitud(
            @NotNull(message = "El ancho es obligatorio.")
            @DecimalMin(value = "0.1", message = "El ancho debe ser mayor que cero.")
            @Digits(integer = 4, fraction = 2, message = "El ancho admite máximo 2 decimales y 4 enteros.")
            BigDecimal ancho,

            @NotNull(message = "El alto es obligatorio.")
            @DecimalMin(value = "0.1", message = "El alto debe ser mayor que cero.")
            @Digits(integer = 4, fraction = 2, message = "El alto admite máximo 2 decimales y 4 enteros.")
            BigDecimal alto,

            @NotNull(message = "La profundidad es obligatoria.")
            @DecimalMin(value = "0.1", message = "La profundidad debe ser mayor que cero.")
            @Digits(integer = 4, fraction = 2, message = "La profundidad admite máximo 2 decimales y 4 enteros.")
            BigDecimal profundidad) {
    }
}
