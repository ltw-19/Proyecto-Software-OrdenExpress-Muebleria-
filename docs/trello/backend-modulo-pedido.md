# Documentación para Tarjetas de Trello — Backend (Historia: Registrar Pedido)

## Historia: Registrar Pedido (CLIENTE) — Sprint 1

### Decisiones tomadas (antes "sin definir")
- Los ítems del pedido salen del **carrito guardado** en `carritos.json`; el navegador solo envía los datos de entrega.
- Al registrar, el carrito **se vacía** bajo el mismo candado: un doble clic encuentra el carrito vacío y recibe **409**, así que no se duplica el pedido.
- **Primero se registra, después se descarga el PDF:** `POST /api/pedidos` registra y devuelve el enlace de WhatsApp; `GET /api/pedidos/{id}/preorden` descarga la Pre-orden las veces que se quiera sin registrar nada nuevo.

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** en el Carrito, el CLIENTE revisa los datos de entrega precargados y pulsa "Registrar pedido". Luego puede "Descargar Pre-orden" (PDF) y "Enviar Pedido a WhatsApp" (abre `enlaceWhatsapp`) (*pendiente de implementar en el frontend*).
- **Mensajes que ve el usuario:**
  - 409 → "Tu carrito está vacío. Añade al menos un mueble antes de registrar el pedido." (también en el doble clic)
  - 409 → "Stock insuficiente"
  - 409 → "Uno de los muebles de tu carrito ya no está en el catálogo. Elimínalo para continuar."
  - 400 → "El nombre es obligatorio." / "El apellido es obligatorio." / "El teléfono debe tener entre 10 y 15 dígitos (puede iniciar con +)." / "La dirección de entrega es obligatoria." / "La dirección admite máximo 200 caracteres."
  - 404 → "El pedido no existe." · 403 → "No tienes permisos para ver este pedido."
  - 401 / 403 → sesión no válida / "No tienes permisos para realizar esta acción."

### Arquitectura (Capas y Endpoints)
- **Endpoints:**
  - `POST /api/pedidos` → **201 Created** con el pedido y `enlaceWhatsapp`. Solo `CLIENTE`.
  - `GET /api/pedidos/{id}/preorden` → **200** `application/pdf` (`preorden-OE-0001.pdf`). Solo el `CLIENTE` dueño.
- **Flujo de petición:**
  `PedidoControlador` (`@Valid RegistrarPedidoSolicitud`) → `PedidoServicio` → `CarritoRepositorio.procesarItemsYVaciar` (candado de `carritos.json`) → `MuebleRepositorio` (lee `muebles.json`) → `PedidoRepositorio.agregarConNumeroNuevo` (candado de `pedidos.json`) → `ServicioArchivoJson`.
- **PDF:** `PreordenPdfServicio` (OpenPDF 1.3.43, `com.lowagie`) dibuja la Pre-orden con los montos congelados.
- **WhatsApp:** `https://wa.me/<app.whatsapp.numero>?text=<mensaje codificado>`; el número es uno solo y está en `application.properties`.
- **Seguridad:** `idUsuario` sale del token. El cuerpo no lleva precios ni ítems; si se envían `totalUsd` o `idUsuario`, se ignoran.

### Estructura de Datos
- **Petición** (`Authorization: Bearer <token del CLIENTE>`):
```json
{
  "nombre": "Ana",
  "apellido": "Pérez",
  "telefono": "04121234567",
  "direccion": "Av. Principal, Caracas"
}
```
- **Nodo guardado en `pedidos.json`:**
```json
{
  "id" : "0caa5e10-caed-4a0f-8b85-979a9f913789",
  "numero" : "OE-0001",
  "idUsuario" : "65fb0562-a877-4502-b9f8-8d78e49bad3c",
  "items" : [ {
    "idMueble" : "ddd9afb1-05cb-4ca4-bcdb-8de4b2756a9f",
    "codigo" : "M-001",
    "nombre" : "Sofá Alba",
    "cantidad" : 2,
    "precioUnitarioUsd" : 480.00,
    "precioUnitarioBs" : 19200.00,
    "subtotalUsd" : 960.00,
    "subtotalBs" : 38400.00
  } ],
  "tasaBcv" : 40.00,
  "totalUsd" : 960.00,
  "totalBs" : 38400.00,
  "entrega" : {
    "nombre" : "Ana",
    "apellido" : "Pérez",
    "telefono" : "04121234567",
    "direccion" : "Av. Principal, Caracas"
  },
  "estado" : "PENDIENTE",
  "timestamp" : "2026-11-08T04:15:18.630013400Z"
}
```
- **Nodo de `carritos.json` después de registrar** (vacío, sin `null`):
```json
{ "idUsuario" : "65fb0562-a877-4502-b9f8-8d78e49bad3c", "items" : [ ], "fechaActualizacion" : "2026-11-08T04:15:18.635158700Z" }
```
- **Respuesta (201 Created):** el mismo pedido (sin `idUsuario`) más `"enlaceWhatsapp": "https://wa.me/584120000000?text=Hola%2C%20quiero%20confirmar%20mi%20pre-orden%20OE-0001..."`.

### Algoritmo
1. `@Valid` revisa los datos de entrega (nombre, apellido, teléfono `^\+?[0-9]{10,15}$`, dirección obligatoria ≤ 200) → si fallan, **400**. El servicio recorta los espacios.
2. `CarritoRepositorio.procesarItemsYVaciar` toma el candado de `carritos.json` y entrega una copia de los ítems del cliente (id del token).
3. Si no hay ítems → **409** (carrito vacío). Así se cubre también el doble clic.
4. Lee la tasa BCV **una sola vez** (tasa congelada del pedido).
5. Para cada ítem lee el mueble en `muebles.json`: si no existe → **409**; si `cantidad > unidades` → **409 "Stock insuficiente"**. El stock **no** se descuenta (se descuenta cuando el GESTOR aprueba).
6. Congela los montos con `BigDecimal`, 2 decimales, `HALF_UP`: `subtotalUsd = precioUsd × cantidad`, `totalUsd = Σ subtotalUsd`, `Bs = monto USD × tasa` (el total en Bs. sale del total en USD).
7. `PedidoRepositorio.agregarConNumeroNuevo`, bajo el candado de `pedidos.json`: número = mayor `OE-####` + 1, id UUID, estado `PENDIENTE`, `timestamp` inmutable. Lo guarda.
8. De vuelta en el candado del carrito, deja `items: []` y guarda `carritos.json`.
9. **Compensación:** si falla guardar el carrito, se borra el pedido recién creado y se responde **500**, para no dejar un pedido con el carrito todavía lleno. Si cualquier validación falla (pasos 3 a 5), ningún archivo cambia.
10. Responde **201** con el pedido y el enlace de WhatsApp (mensaje con número, cliente, teléfono, dirección, productos, totales y tasa).
11. **Pre-orden:** `GET /api/pedidos/{id}/preorden` busca el pedido (no existe → **404**; es de otro cliente → **403**) y genera el PDF con los montos congelados.

### Pruebas
- Postman: `docs/pruebas/pedido-registrar.postman_collection.json` (25 peticiones).
- JUnit: `RegistrarPedidoTest` (montos congelados, numeración, carrito vacío, doble registro, stock insuficiente, permisos de la Pre-orden).
