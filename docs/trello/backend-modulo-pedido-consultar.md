# Documentación para Tarjetas de Trello — Backend (Historia: Consultar Pedido, con Aprobar y Rechazar)

## Historia: Consultar Pedido (TODOS) — Sprint 1

### Alcance
- **CLIENTE:** ve solo sus pedidos ("Mis pedidos") y el detalle de cada uno.
- **ADMIN y GESTOR:** ven la **bandeja global** (todos los pedidos) y el detalle de cualquiera.
- **Aprobar / Rechazar:** solo el **GESTOR**, solo desde `PENDIENTE`. El ADMIN solo consulta.
- **Stock:** se descuenta al **aprobar** (todo el pedido o nada). Rechazar no toca el stock.

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** "Mis pedidos" (CLIENTE) y "Pedidos recibidos" (ADMIN, GESTOR) muestran número, fecha, estado (`PENDIENTE` amarillo, `APROBADO` verde, `RECHAZADO` rojo) y totales en USD y Bs. Al abrir un pedido se ve su detalle (ítems, tasa congelada, datos de entrega). En la bandeja, el GESTOR tiene los botones "Aprobar" y "Rechazar" solo en pedidos `PENDIENTE`; el ADMIN no los tiene (*pendiente de implementar en el frontend*).
- **Mensajes que ve el usuario:**
  - 409 → "Solo se puede aprobar un pedido PENDIENTE. Este pedido ya está APROBADO." (también el doble clic)
  - 409 → "Solo se puede rechazar un pedido PENDIENTE. Este pedido ya está RECHAZADO."
  - 409 → "Stock insuficiente"
  - 409 → "Uno de los muebles del pedido ya no está en el catálogo. No se puede aprobar."
  - 404 → "El pedido no existe." · 403 → "No tienes permisos para ver este pedido."
  - 401 / 403 → sesión no válida / "No tienes permisos para realizar esta acción." (ADMIN o CLIENTE que intenta aprobar o rechazar)

### Arquitectura (Capas y Endpoints)
- **Endpoints:**
  - `GET /api/pedidos` → **200**, lista del más reciente al más antiguo. CLIENTE: los suyos; ADMIN y GESTOR: todos.
  - `GET /api/pedidos/{id}` → **200**, detalle. CLIENTE solo el suyo (403 si es ajeno); 404 si no existe.
  - `PUT /api/pedidos/{id}/aprobar` → **200** con el pedido `APROBADO`. Solo `GESTOR`. Sin cuerpo.
  - `PUT /api/pedidos/{id}/rechazar` → **200** con el pedido `RECHAZADO`. Solo `GESTOR`. Sin cuerpo.
- **Flujo de petición:**
  - Consultar: `PedidoControlador` → `PedidoServicio.consultar / consultarDetalle` → `PedidoRepositorio.listar / buscarPorId` → `ServicioArchivoJson` (lee `pedidos.json`).
  - Aprobar: `PedidoControlador` → `PedidoServicio.aprobar` → `PedidoRepositorio.actualizar` (candado de `pedidos.json`) → `MuebleRepositorio.modificar` (candado de `muebles.json`) → `ServicioArchivoJson`.
  - Rechazar: `PedidoControlador` → `PedidoServicio.rechazar` → `PedidoRepositorio.actualizar` (candado de `pedidos.json`).
- **Seguridad:** permisos con `@PreAuthorize`; el id y el rol de quien consulta salen del token (el rol se relee de `usuarios.json` en cada petición), nunca de la URL ni del cuerpo.
- **Respuesta:** mismo formato que Registrar Pedido (`PedidoRespuesta`), con los montos **congelados** al registrar. No expone `idUsuario`.

### Estructura de Datos
- **Respuesta de `GET /api/pedidos`** (arreglo; cada elemento es igual al detalle):
```json
[ {
  "id" : "5402b208-2f57-4514-af67-6f095270e837",
  "numero" : "OE-0001",
  "estado" : "APROBADO",
  "timestamp" : "2026-11-08T23:11:17.021790600Z",
  "items" : [ {
    "idMueble" : "e27b9025-ed5c-4a0c-b746-29020103bf4c",
    "codigo" : "M-002",
    "nombre" : "Butaca Nido",
    "cantidad" : 1,
    "precioUnitarioUsd" : 180.00,
    "precioUnitarioBs" : 7200.00,
    "subtotalUsd" : 180.00,
    "subtotalBs" : 7200.00
  } ],
  "tasaBcv" : 40.00,
  "totalUsd" : 180.00,
  "totalBs" : 7200.00,
  "entrega" : {
    "nombre" : "Cliente",
    "apellido" : "Consulta",
    "telefono" : "04121234567",
    "direccion" : "Av. Principal, Caracas"
  },
  "enlaceWhatsapp" : "https://wa.me/584120000000?text=Hola%2C%20quiero%20confirmar..."
} ]
```
- **Nodo de `pedidos.json` después de aprobar:** igual al que dejó Registrar Pedido; **solo cambia `estado`** (`"PENDIENTE"` → `"APROBADO"` o `"RECHAZADO"`). `timestamp`, montos y tasa no cambian.
- **Nodo de `muebles.json` después de aprobar** (solo cambia `unidades`; ejemplo con 9 unidades y un pedido de 1):
```json
{ "id" : "e27b9025-ed5c-4a0c-b746-29020103bf4c", "codigo" : "M-002", "nombre" : "Butaca Nido", "...": "...", "precioUsd" : 180.00, "unidades" : 8 }
```

### Algoritmo
**Consultar (lista):**
1. Lee `pedidos.json`.
2. Si el rol es `CLIENTE`, se queda con los pedidos cuyo `idUsuario` es el del token. ADMIN y GESTOR reciben todos.
3. Ordena por `timestamp` descendente (más reciente primero) y responde **200**.

**Consultar (detalle):**
1. Busca el pedido por id → si no existe, **404**.
2. Si el rol es `CLIENTE` y el pedido es de otra persona → **403**. ADMIN y GESTOR ven cualquiera.

**Aprobar (GESTOR):**
1. Toma el candado de `pedidos.json` y lee la versión más reciente del pedido → no existe: **404**.
2. Si el estado no es `PENDIENTE` → **409**. Esto evita que un doble clic (o dos gestores a la vez) descuente el stock dos veces: el segundo ya ve `APROBADO`.
3. Toma el candado de `muebles.json` y revisa **todos** los ítems: mueble inexistente → **409**; cantidad mayor que las unidades actuales → **409 "Stock insuficiente"**. Si uno falla, no se descuenta nada y el pedido sigue `PENDIENTE`.
4. Si todos alcanzan, resta las unidades y guarda `muebles.json`.
5. Cambia el estado a `APROBADO` y guarda `pedidos.json`.
6. **Compensación:** si falla guardar `pedidos.json`, devuelve las unidades descontadas en el paso 4 y responde **500**, para que el stock no quede rebajado con el pedido todavía `PENDIENTE`.

**Rechazar (GESTOR):**
1. Bajo el candado de `pedidos.json`: no existe → **404**; no está `PENDIENTE` → **409**.
2. Cambia el estado a `RECHAZADO`. No toca el stock (nunca se descontó).

### Pruebas
- Postman: `docs/pruebas/pedido-consultar.postman_collection.json` (42 peticiones, 69 verificaciones; ejecutada dos veces seguidas con Newman sin fallos). El bloque final deja en 0 el mueble de menos stock: para recuperarlo, borrar `datos/` y reiniciar.
- JUnit: `ConsultarPedidoTest` (filtro por cliente y orden, permisos del detalle, descuento único al aprobar, todo o nada sin stock, rechazar sin tocar stock).
