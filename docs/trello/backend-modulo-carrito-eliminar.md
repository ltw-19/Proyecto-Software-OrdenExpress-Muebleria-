# Documentación para Tarjetas de Trello — Backend (Historia: Eliminar ítem del Carrito)

## Historia: Eliminar ítem del Carrito (CLIENTE) — Sprint 1

### Reglas
- Solo el **CLIENTE**, y solo en **su** carrito (el dueño sale del token).
- Se quita el ítem completo (no se resta cantidad: los botones − / + son Sprint 2). Para cambiar la cantidad, el cliente elimina el ítem y lo vuelve a añadir.
- **Stock:** no cambia (nunca se descontó al añadir; se descuenta cuando el GESTOR aprueba el pedido).
- Se puede quitar un mueble aunque ya no exista en el catálogo.

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** en la pantalla "Carrito", cada ítem tiene un botón "Eliminar". Al pulsarlo, el frontend llama a `DELETE /api/carrito/items/{idMueble}` y redibuja el carrito con la respuesta: los totales ya vienen recalculados por el backend (*pendiente de implementar en el frontend*).
- **Mensajes que ve el usuario:**
  - 200 → carrito actualizado (si era el último ítem, carrito vacío)
  - 404 → "Este mueble no está en tu carrito." (también en el doble clic)
  - 403 → "No tienes permisos para realizar esta acción." (ADMIN o GESTOR)
  - 401 → sesión no válida

### Arquitectura (Capas y Endpoints)
- **Endpoint:** `DELETE /api/carrito/items/{idMueble}` → **200 OK** con el carrito completo. Solo rol `CLIENTE` (`@PreAuthorize("hasRole('CLIENTE')")`).
- **Flujo de petición:** `CarritoControlador.eliminarItem` → `CarritoServicio.eliminarItem` → `CarritoRepositorio.eliminarItem` (busca y quita bajo el candado de `carritos.json`) → `ServicioArchivoJson`. Para la respuesta, `MuebleRepositorio` (precios actuales) y `ConversionMonedaServicio` (bolívares con la tasa BCV).
- **Seguridad:** el carrito que se modifica es SIEMPRE el del token. Un cliente no puede tocar el carrito de otro: si pide un mueble que no está en *su* carrito recibe 404.
- **Concurrencia:** la búsqueda y el guardado ocurren bajo el **mismo candado**: en un doble clic la primera petición elimina y la segunda recibe 404, sin dañar el archivo.

### Estructura de Datos
- **Petición:** sin cuerpo. `Authorization: Bearer <token del CLIENTE>` y el id del mueble en la URL: `DELETE /api/carrito/items/6d10ae46-2ea0-48ed-b2a4-5c7e1b185d2e`.
- **Nodo en `carritos.json`** (el ítem se **quita** del arreglo; no queda `null`; si era el último, `items` queda `[]`):
```json
// antes
{ "idUsuario" : "e8153801-...", "items" : [
    { "idMueble" : "6d10ae46-...", "cantidad" : 2, "fechaAgregado" : "2026-11-07T20:47:44Z" },
    { "idMueble" : "a3f2c9b1-...", "cantidad" : 1, "fechaAgregado" : "2026-11-07T20:50:10Z" } ],
  "fechaActualizacion" : "2026-11-07T20:50:10Z" }
// después de DELETE /api/carrito/items/6d10ae46-...
{ "idUsuario" : "e8153801-...", "items" : [
    { "idMueble" : "a3f2c9b1-...", "cantidad" : 1, "fechaAgregado" : "2026-11-07T20:50:10Z" } ],
  "fechaActualizacion" : "2026-11-08T23:38:40Z" }
```
- **Respuesta (200 OK):** mismo formato que `GET /api/carrito`, con los totales recalculados:
```json
{
  "items": [
    { "idMueble": "a3f2c9b1-...", "codigo": "M-002", "nombre": "Mesa Roble", "urlFoto": "https://...",
      "precioUsd": 19.99, "precioBs": 799.60, "cantidad": 1,
      "subtotalUsd": 19.99, "subtotalBs": 799.60, "disponible": true }
  ],
  "cantidadItems": 1,
  "totalUsd": 19.99,
  "totalBs": 799.60
}
```
- **Carrito vacío:** `{"items": [], "cantidadItems": 0, "totalUsd": 0.00, "totalBs": 0.00}`.
- **Errores:** `{"mensaje": "..."}`. **`muebles.json`:** no cambia.

### Algoritmo
1. `@PreAuthorize` revisa el rol: ADMIN o GESTOR → **403** (no llega al servicio).
2. El servicio verifica que venga el id del mueble (si no → **400**).
3. Bajo el candado de `carritos.json` busca el carrito del cliente del token. Si no tiene carrito → **404** (no se le crea uno).
4. Recorre los ítems y arma una lista nueva **sin** ese mueble (de paso descarta ítems `null` o dañados).
5. Si el mueble no estaba → **404** "Este mueble no está en tu carrito." y el contenido no cambia.
6. Si estaba, guarda el carrito con la lista nueva y la fecha de actualización (escritura atómica con archivo temporal).
7. Recalcula subtotales y totales con los precios ACTUALES de `muebles.json`; los bolívares se obtienen convirtiendo el total en USD (no sumando Bs. redondeados).
8. Responde **200** con el carrito actualizado.

### Pruebas
- Postman: `docs/pruebas/carrito-eliminar.postman_collection.json` (21 peticiones, 39 verificaciones; ejecutada dos veces seguidas con Newman sin fallos). No modifica el stock.
- JUnit: `EliminarItemCarritoTest` (quita el ítem y recalcula totales, el último ítem deja `items: []` sin `null`, doble clic 404, cliente sin carrito 404, no toca carritos ajenos, quita muebles que ya no existen, no cambia el stock, id vacío 400).
