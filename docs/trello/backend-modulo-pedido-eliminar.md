# Documentación para Tarjetas de Trello — Backend (Historia: Eliminar Pedido)

## Historia: Eliminar Pedido (CLIENTE, GESTOR) — Sprint 1

### Reglas
- **CLIENTE:** solo sus pedidos y solo en estado `PENDIENTE`. Ajeno o ya decidido (`APROBADO` / `RECHAZADO`) → **403**.
- **GESTOR:** cualquier pedido, en cualquier estado.
- **ADMIN:** no puede eliminar → **403**.
- **Stock:** eliminar **no devuelve** unidades (si el pedido estaba `APROBADO`, el descuento se mantiene).

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** en "Mis pedidos" el CLIENTE ve el botón "Eliminar" solo en sus pedidos `PENDIENTE`; en "Pedidos recibidos" el GESTOR lo ve en todos; el ADMIN no lo ve. Antes de borrar se pide confirmación en pantalla (*pendiente de implementar en el frontend*).
- **Mensajes que ve el usuario:**
  - 204 → sin cuerpo (el pedido se quita de la lista)
  - 403 → "No tienes permisos para eliminar este pedido." (CLIENTE con pedido ajeno)
  - 403 → "Solo puedes eliminar pedidos PENDIENTES. Este pedido ya está APROBADO." (o RECHAZADO)
  - 403 → "No tienes permisos para realizar esta acción." (ADMIN)
  - 404 → "El pedido no existe." (también en el doble clic) · 401 → sesión no válida

### Arquitectura (Capas y Endpoints)
- **Endpoint:** `DELETE /api/pedidos/{id}` → **204 No Content**. Roles `CLIENTE` y `GESTOR` (`@PreAuthorize("hasAnyRole('CLIENTE', 'GESTOR')")`).
- **Flujo de petición:** `PedidoControlador.eliminar` → `PedidoServicio.eliminar` (reglas por rol) → `PedidoRepositorio.eliminar` (revisa y borra bajo el candado de `pedidos.json`) → `ServicioArchivoJson`.
- **Seguridad:** el id y el rol de quien elimina salen del token (el rol se relee de `usuarios.json`), nunca de la URL ni del cuerpo.
- **Concurrencia:** la revisión de permisos y el borrado ocurren bajo el **mismo candado**. Si el GESTOR aprueba el pedido mientras el cliente lo elimina, el que llega segundo ve el estado ya actualizado.

### Estructura de Datos
- **Petición:** sin cuerpo. `Authorization: Bearer <token>` y el id en la URL: `DELETE /api/pedidos/5402b208-2f57-4514-af67-6f095270e837`.
- **Respuesta:** `204 No Content` sin cuerpo; errores con `{"mensaje": "..."}`.
- **`pedidos.json`:** el nodo completo del pedido se **quita** del arreglo (no se marca como borrado ni deja `null`):
```json
// antes
[ { "id" : "5402b208-...", "numero" : "OE-0001", "estado" : "PENDIENTE", "...": "..." },
  { "id" : "9b1c7f3e-...", "numero" : "OE-0002", "estado" : "PENDIENTE", "...": "..." } ]
// después de DELETE /api/pedidos/5402b208-...
[ { "id" : "9b1c7f3e-...", "numero" : "OE-0002", "estado" : "PENDIENTE", "...": "..." } ]
```
- **Numeración:** el siguiente pedido sigue siendo "el mayor `OE-####` existente + 1". Si se elimina el último, su número puede volver a usarse.
- **`muebles.json`:** no cambia.

### Algoritmo
1. `@PreAuthorize` revisa el rol: ADMIN → **403** (no llega al servicio).
2. El servicio valida que venga el id y pide al repositorio borrar con una revisión previa.
3. Bajo el candado de `pedidos.json` busca el pedido → no existe: **404**.
4. Si el rol es `CLIENTE`: el pedido debe ser suyo (si no → **403**) y estar `PENDIENTE` (si no → **403**).
5. Si pasa la revisión, quita el nodo y guarda `pedidos.json` (escritura atómica con archivo temporal). Si la revisión falla, el archivo no cambia.
6. No toca `muebles.json`: el stock no se devuelve.
7. Responde **204** sin cuerpo y registra en el log el número del pedido y el rol de quien lo eliminó.

### Pruebas
- Postman: `docs/pruebas/pedido-eliminar.postman_collection.json` (34 peticiones, 46 verificaciones; ejecutada dos veces seguidas con Newman sin fallos). Cada corrida descuenta 1 unidad del mueble de más stock.
- JUnit: `EliminarPedidoTest` (el cliente elimina su pedido pendiente, no elimina ajenos ni decididos, el gestor elimina cualquiera sin devolver stock, 404 si no existe).
