# Documentación para Tarjetas de Trello — Frontend (Módulo de Pedidos: Registrar y Mis Pedidos)

## 8. Historia: Registrar Pedido (CLIENTE) — Sprint 1

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaCarrito.vue`:
    - Formulario de entrega prellenado automáticamente desde `sesion.usuario` (nombre, apellido, teléfono, dirección).
    - Validación reactiva del 100% de los campos con mensajes de error contextuales.
    - Botón "Registrar Pedido".
    - Sección de confirmación de pedido con número `OE-####`, montos congelados, botón para **Descargar Pre-orden (PDF)** y botón exclusivo con **verde `#25D366`** para **"Enviar Pedido a WhatsApp"**.
- **Ruta frontend:** `/carrito`

### Arquitectura (Capas y Endpoints)
- **Capa y servicios:**
  - `VistaCarrito.vue` → `pedidoServicio.registrarPedido()`, `pedidoServicio.descargarPreordenPdf()` → `api.js`.
- **Endpoints consumidos:**
  - `POST /api/pedidos` (201 Created con `PedidoRespuesta` y `enlaceWhatsapp`).
  - `GET /api/pedidos/{id}/preorden` (200 OK con `application/pdf`, descargado como blob).

### Estructura de Datos
- **JSON enviado (`POST /api/pedidos`):**
```json
{
  "nombre": "Ana",
  "apellido": "Pérez",
  "telefono": "04121234567",
  "direccion": "Av. Principal, Caracas"
}
```
- **JSON recibido (`PedidoRespuesta` - 201 Created):**
```json
{
  "id": "0caa5e10-caed-4a0f-8b85-979a9f913789",
  "numero": "OE-0001",
  "estado": "PENDIENTE",
  "timestamp": "2026-11-08T04:15:18.630013400Z",
  "items": [
    {
      "idMueble": "ddd9afb1-05cb-4ca4-bcdb-8de4b2756a9f",
      "codigo": "M-001",
      "nombre": "Sofá Alba",
      "cantidad": 2,
      "precioUnitarioUsd": 480.00,
      "precioUnitarioBs": 19200.00,
      "subtotalUsd": 960.00,
      "subtotalBs": 38400.00
    }
  ],
  "tasaBcv": 40.00,
  "totalUsd": 960.00,
  "totalBs": 38400.00,
  "entrega": {
    "nombre": "Ana",
    "apellido": "Pérez",
    "telefono": "04121234567",
    "direccion": "Av. Principal, Caracas"
  },
  "enlaceWhatsapp": "https://wa.me/584120000000?text=Hola%2C%20quiero%20confirmar%20mi%20pre-orden%20OE-0001..."
}
```

### Algoritmo
1. El cliente valida y edita los datos de entrega en el formulario del carrito.
2. Se realiza validación previa en el cliente:
   - Nombre y apellido obligatorios (≤ 100 caracteres).
   - Teléfono obligatorio con regex `^\+?[0-9]{10,15}$` tras remover espacios y guiones.
   - Dirección obligatoria (≤ 200 caracteres).
3. Se despacha `POST /api/pedidos`.
4. El backend registra el pedido, congela precios y vacía el carrito en `carritos.json`.
5. Al recibir 201 Created:
   - Se limpia el estado reactivo del carrito (`carritoEstado.vaciar()`).
   - Se muestra la sección de éxito con el número asignado (`OE-####`).
   - El cliente puede pulsar "Descargar Pre-orden (PDF)" o "Enviar Pedido a WhatsApp" inmediatamente.

---

## 9. Historia: Consultar y Eliminar Pedido propio (CLIENTE: Mis Pedidos) — Sprint 1

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaMisPedidos.vue`:
    - Listado de pedidos propios del cliente ordenados del más reciente al más antiguo.
    - Encabezado con número de pedido, fecha y hora formateada en español.
    - Insignias de estado según reglas de diseño:
      - `PENDIENTE` en amarillo.
      - `APROBADO` en verde.
      - `RECHAZADO` en rojo.
    - Totales en USD y Bs. con `Intl.NumberFormat('es-VE')`.
    - Botón para desplegar/ocultar el detalle completo (ítems, tasa congelada y datos de entrega).
    - Botón para re-descargar la Pre-orden en PDF en cualquier momento.
    - Botón para enviar mensaje por WhatsApp con verde `#25D366`.
    - **Botón "Eliminar Pedido" visible ÚNICAMENTE en pedidos con estado `PENDIENTE`**, con modal de confirmación.
  - `BarraNavegacion.vue`: Enlace directo a "Mis Pedidos" para usuarios con rol `CLIENTE`.
- **Ruta frontend:** `/mis-pedidos` (requiere autenticación, rol `CLIENTE`).

### Arquitectura (Capas y Endpoints)
- **Capa y servicios:**
  - `VistaMisPedidos.vue` → `pedidoServicio.consultarPedidos()`, `pedidoServicio.eliminarPedido()`, `pedidoServicio.descargarPreordenPdf()` → `api.js`.
- **Endpoints consumidos:**
  - `GET /api/pedidos` (200 OK con arreglo de `PedidoRespuesta`).
  - `DELETE /api/pedidos/{id}` (204 No Content).
  - `GET /api/pedidos/{id}/preorden` (200 OK con blob PDF).

### Algoritmo
1. Al montarse la vista, se ejecuta `pedidoServicio.consultarPedidos()`.
2. El backend filtra los pedidos basándose en el `idUsuario` del token JWT y devuelve solo los del cliente en orden descendente por fecha.
3. Se presentan las tarjetas responsivas con las insignias de color correspondientes.
4. Si el pedido se encuentra en estado `PENDIENTE`, se habilita el botón "Eliminar".
5. Al pulsar "Eliminar", se solicita confirmación modal para prevenir borrados accidentales.
6. Al confirmar, se envía `DELETE /api/pedidos/{id}`; al recibir 204 No Content, el pedido se retira de la lista reactiva en pantalla y se notifica con una alerta de éxito.
