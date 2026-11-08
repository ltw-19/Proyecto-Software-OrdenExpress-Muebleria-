package com.ordenexpress.orden_express_muebleria.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regla de negocio bimonetaria: convierte dólares a bolívares con la tasa oficial
 * del BCV (propiedad tasa.bcv). Usa BigDecimal para no perder centavos.
 */
@Service
public class ConversionMonedaServicio {

    private final BigDecimal tasaBcv;

    public ConversionMonedaServicio(@Value("${tasa.bcv}") BigDecimal tasaBcv) {
        if (tasaBcv == null || tasaBcv.signum() <= 0) {
            throw new IllegalStateException("La propiedad tasa.bcv debe ser un número mayor que cero.");
        }
        this.tasaBcv = tasaBcv;
    }

    /**
     * Convierte USD a Bs. redondeando a 2 decimales (HALF_UP).
     * Para totales, convierte el TOTAL en USD (no sumes montos ya redondeados en Bs.)
     * y así el resultado no acumula errores de centavos.
     */
    public BigDecimal usdABolivares(BigDecimal montoUsd) {
        return usdABolivares(montoUsd, tasaBcv);
    }

    /**
     * Igual que la anterior, pero con una tasa dada. La usa "Registrar Pedido" para que todos
     * los montos de un pedido salgan de la MISMA tasa congelada, aunque la tasa cambie a mitad del cálculo.
     */
    public static BigDecimal usdABolivares(BigDecimal montoUsd, BigDecimal tasa) {
        return montoUsd.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTasaBcv() {
        return tasaBcv;
    }
}
