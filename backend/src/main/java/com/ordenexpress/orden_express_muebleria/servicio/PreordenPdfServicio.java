package com.ordenexpress.orden_express_muebleria.servicio;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ordenexpress.orden_express_muebleria.excepcion.ExcepcionNegocio;
import com.ordenexpress.orden_express_muebleria.modelo.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dibuja la Pre-orden en PDF (OpenPDF, paquete com.lowagie) a partir de un pedido YA guardado.
 * Solo usa los montos congelados del pedido: aquí no se calcula ningún precio ni tasa.
 * La Pre-orden NO es una factura ni un comprobante de pago (no hay pasarela de pago).
 */
@Service
public class PreordenPdfServicio {

    private static final Logger registro = LoggerFactory.getLogger(PreordenPdfServicio.class);

    private static final ZoneId ZONA_VENEZUELA = ZoneId.of("America/Caracas");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Paleta del proyecto: nogal oscuro para títulos y encabezados de tabla. */
    private static final Color NOGAL = new Color(0x5D, 0x40, 0x37);

    public byte[] generar(Pedido pedido) {
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NOGAL);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NOGAL);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font encabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font nota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Galería de Casa", titulo));
            documento.add(new Paragraph("Pre-orden " + pedido.numero(), subtitulo));
            documento.add(new Paragraph("Fecha: " + FORMATO_FECHA.format(pedido.timestamp().atZone(ZONA_VENEZUELA))
                    + "    Estado: " + pedido.estado(), normal));
            documento.add(new Paragraph(" "));

            Pedido.DatosEntrega entrega = pedido.entrega();
            documento.add(new Paragraph("Datos de entrega", subtitulo));
            documento.add(new Paragraph("Cliente: " + entrega.nombre() + " " + entrega.apellido(), normal));
            documento.add(new Paragraph("Teléfono: " + entrega.telefono(), normal));
            documento.add(new Paragraph("Dirección: " + entrega.direccion(), normal));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{1.2f, 3.2f, 0.8f, 1.5f, 1.8f, 1.5f, 1.8f});
            tabla.setWidthPercentage(100);
            for (String columna : new String[]{"Código", "Mueble", "Cant.", "Precio USD", "Precio Bs.",
                    "Subtotal USD", "Subtotal Bs."}) {
                PdfPCell titulo1 = new PdfPCell(new Phrase(columna, encabezado));
                titulo1.setBackgroundColor(NOGAL);
                titulo1.setPadding(4);
                tabla.addCell(titulo1);
            }
            for (Pedido.ItemPedido item : pedido.items()) {
                tabla.addCell(texto(item.codigo(), celda));
                tabla.addCell(texto(item.nombre(), celda));
                tabla.addCell(numero(String.valueOf(item.cantidad()), celda));
                tabla.addCell(numero(monto(item.precioUnitarioUsd()), celda));
                tabla.addCell(numero(monto(item.precioUnitarioBs()), celda));
                tabla.addCell(numero(monto(item.subtotalUsd()), celda));
                tabla.addCell(numero(monto(item.subtotalBs()), celda));
            }
            documento.add(tabla);
            documento.add(new Paragraph(" "));

            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.add(new Phrase("Total USD: " + monto(pedido.totalUsd()) + "\n", negrita));
            totales.add(new Phrase("Total Bs.: " + monto(pedido.totalBs()) + "\n", negrita));
            totales.add(new Phrase("Tasa aplicada: Bs. " + monto(pedido.tasaBcv()) + " por USD", normal));
            documento.add(totales);
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Este documento es una Pre-orden: no es una factura ni un comprobante de pago. "
                    + "Envíala por WhatsApp al Gestor de Ventas para coordinar el pago y la entrega. "
                    + "Los precios y la tasa quedaron congelados al registrar el pedido.", nota));

            documento.close();
        } catch (DocumentException e) {
            registro.error("No se pudo generar la Pre-orden del pedido {}", pedido.numero(), e);
            throw new ExcepcionNegocio(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar la Pre-orden. Intenta de nuevo.");
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private static PdfPCell texto(String valor, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(valor == null ? "" : valor, fuente));
        celda.setPadding(4);
        return celda;
    }

    private static PdfPCell numero(String valor, Font fuente) {
        PdfPCell celda = texto(valor, fuente);
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    /** Formato venezolano: 1.234,50. */
    private static String monto(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-VE"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }
}
