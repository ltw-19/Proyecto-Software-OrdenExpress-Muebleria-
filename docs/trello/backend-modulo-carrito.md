# Documentación para Tarjetas de Trello — Backend (Historia: Añadir ítem al Carrito)

## Historia: Añadir ítem al Carrito (CLIENTE) — Sprint 1

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** desde el catálogo, el CLIENTE elige la cantidad y pulsa "Añadir al carrito". El frontend llama a `POST /api/carrito/items` y muestra el `mensaje` de la respuesta si hay error (*pendiente de implementar en el frontend*).
- **Mensajes que ve el usuario:**
  - 409 → "Stock insuficiente"
  - 409 → "Este mueble ya está en tu carrito. Elimínalo para añadirlo con otra cantidad."
  - 404 → "El mueble no existe."
  - 400 → "La cantidad es obligatoria." / "La cantidad debe ser al menos 1." / "La cantidad admite máximo 100000." / "El mueble es obligatorio."
  - 400 → "La cantidad debe ser un número entero, sin decimales ni comillas." (por ejemplo `1.5`, `"2"`, `1e2`)
  - 401 / 403 → sesión no válida / "No tienes permisos para realizar esta acción."

### Arquitectura (Capas y Endpoints)
- **Endpoint:** `POST /api/carrito/items` → **201 Created** con el carrito completo. Solo rol `CLIENTE` (`@PreAuthorize("hasRole('CLIENTE')")`).
- **Flujo de petición:**
  `CarritoControlador` (+ `DeserializadorCantidadEntera` y `@Valid`) → `CarritoServicio` → `MuebleRepositorio` (lee `muebles.json`) + `CarritoRepositorio` (lee y escribe `carritos.json`) → `ServicioArchivoJson`.
- **Montos:** `ConversionMonedaServicio` calcula los bolívares (tasa BCV, `BigDecimal`, 2 decimales, `HALF_UP`).
- **Seguridad:** el dueño del carrito (`idUsuario`) sale del token JWT, nunca del cuerpo ni de la URL. El navegador no envía precios.

### Estructura de Datos
- **Petición:**
  - Cabecera: `Authorization: Bearer <token del CLIENTE>`
```json
{
  "idMueble": "6d10ae46-2ea0-48ed-b2a4-5c7e1b185d2e",
  "cantidad": 2
}
```
- **Nodo guardado en `carritos.json`** (un carrito por cliente; sin precios ni totales, se calculan al responder):
```json
{
  "idUsuario" : "e8153801-e228-4483-a679-bfa64635dcc8",
  "items" : [ {
    "idMueble" : "6d10ae46-2ea0-48ed-b2a4-5c7e1b185d2e",
    "cantidad" : 2,
    "fechaAgregado" : "2026-11-07T20:47:44.227771625Z"
  } ],
  "fechaActualizacion" : "2026-11-07T20:47:44.227771625Z"
}
```
- **Respuesta (201 Created):**
```json
{
  "items": [
    {
      "idMueble": "6d10ae46-2ea0-48ed-b2a4-5c7e1b185d2e",
      "codigo": "M-001",
      "nombre": "Sofá Alba",
      "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Sofa+Alba",
      "precioUsd": 480.00,
      "precioBs": 19200.00,
      "cantidad": 2,
      "subtotalUsd": 960.00,
      "subtotalBs": 38400.00
    }
  ],
  "cantidadItems": 1,
  "totalUsd": 960.00,
  "totalBs": 38400.00
}
```

### Algoritmo
1. **Validar Cantidad (formato):** al leer el JSON, `DeserializadorCantidadEntera` solo acepta un número entero escrito como número. `1.5`, `2.0`, `"2"`, `"abc"`, `true` o `1e2` → **400** "La cantidad debe ser un número entero, sin decimales ni comillas." (Sin esto, Jackson recortaba `1.5` a `1` en silencio.)
2. **Validar Cantidad (valor):** `@Valid` en `CarritoControlador` revisa el DTO: `idMueble` obligatorio; `cantidad` no nula (`@NotNull`), positiva (`@Min(1)`: `0` y `-1` → **400**) y máximo 100000 (`@Max`). El id del cliente sale del token.
3. **Verificar Datos (servicio):** `CarritoServicio.agregarItem` vuelve a comprobar `idMueble` no vacío y `1 ≤ cantidad ≤ 100000` **antes** de mirar el stock. Así una cantidad inválida nunca llega a la validación de stock, aunque alguien llame al servicio sin pasar por el controlador (lo cubre `CarritoServicioTest`).
4. Busca el mueble en `muebles.json`. Si no existe → **404**.
5. Si `cantidad > unidades` del mueble → **409 "Stock insuficiente"**. El stock **no** se descuenta aquí (se descuenta cuando el GESTOR aprueba el pedido).
6. `CarritoRepositorio.agregarItemSiNoExiste`, dentro de `modificarLista` (un solo candado sobre `carritos.json`):
   - busca el carrito del cliente; si no tiene, lo crea;
   - si el mueble ya está en el carrito → devuelve `YA_EXISTE` y el servicio responde **409** (no hay botones − / +: eso es Sprint 2);
   - si no, añade el ítem `{idMueble, cantidad, fechaAgregado}`, actualiza `fechaActualizacion` y guarda (escritura atómica).
7. El servicio arma la respuesta con los precios **actuales** de `muebles.json`:
   `subtotalUsd = precioUsd × cantidad`; `totalUsd = Σ subtotalUsd`; los Bs. se obtienen convirtiendo cada monto en USD (el total en Bs. sale del total en USD, no de sumar Bs. redondeados).
8. Responde **201 Created** con el carrito.

---

# Documentación para Tarjetas de Trello — Backend (Historia: Consultar Carrito)

## Historia: Consultar Carrito (CLIENTE) — Sprint 1

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** el CLIENTE abre "Mi carrito". El frontend llama a `GET /api/carrito` y muestra cada línea (foto, nombre, código, precio unitario en USD y Bs., cantidad, subtotales) y los totales. Si `disponible` es `false`, muestra "Agotado" junto al ítem. Si `items` está vacío, muestra "Tu carrito está vacío" (*pendiente de implementar en el frontend*).
- **El frontend no calcula montos:** solo muestra los que manda el backend.
- **Mensajes de error:** 401 → "Tu sesión no es válida o expiró. Ingresa de nuevo." / 403 → "No tienes permisos para realizar esta acción." (ADMIN y GESTOR no tienen carrito).

### Arquitectura (Capas y Endpoints)
- **Endpoint:** `GET /api/carrito` → **200 OK**. Solo rol `CLIENTE` (`@PreAuthorize("hasRole('CLIENTE')")`). Sin parámetros: el carrito es SIEMPRE el del usuario del token (no se puede pedir el de otra persona).
- **Flujo de petición:**
  `CarritoControlador.consultar` → `CarritoServicio.consultar` → `CarritoRepositorio.buscarPorUsuario` (solo lee `carritos.json`) + `MuebleRepositorio.listar` (precios actuales de `muebles.json`) → `ServicioArchivoJson`.
- **Montos:** `ConversionMonedaServicio` (tasa BCV, `BigDecimal`, 2 decimales, `HALF_UP`).
- **Solo lectura:** consultar usa `leerLista` (candado de lectura); nunca crea ni modifica carritos.

### Estructura de Datos
- **Petición:** `GET /api/carrito` con cabecera `Authorization: Bearer <token del CLIENTE>`. Sin cuerpo.
- **Nodo leído de `carritos.json`:** el mismo que guarda "Añadir ítem" (`idUsuario`, `items[{idMueble, cantidad, fechaAgregado}]`, `fechaActualizacion`).
- **Respuesta (200 OK) — cliente sin carrito:**
```json
{ "items": [], "cantidadItems": 0, "totalUsd": 0.00, "totalBs": 0.00 }
```
- **Respuesta (200 OK) — con ítems:**
```json
{
  "items": [
    {
      "idMueble": "5f315b94-8099-41c0-8742-af837e2ed4dd",
      "codigo": "M-002",
      "nombre": "Butaca Nido",
      "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Butaca+Nido",
      "precioUsd": 180.00,
      "precioBs": 7200.00,
      "cantidad": 3,
      "subtotalUsd": 540.00,
      "subtotalBs": 21600.00,
      "disponible": true
    }
  ],
  "cantidadItems": 1,
  "totalUsd": 540.00,
  "totalBs": 21600.00
}
```
*(El CLIENTE nunca recibe `unidades`: solo `disponible`.)*

### Algoritmo
1. `CarritoControlador.consultar` toma el id del cliente del token (no hay parámetros).
2. `CarritoServicio.consultar` pide a `CarritoRepositorio.buscarPorUsuario` el carrito de ese id.
3. Si no existe → usa un carrito vacío en memoria (no se escribe nada en el archivo).
4. Lee `muebles.json` una sola vez y arma un mapa `id → mueble`.
5. Recorre los ítems:
   - ignora ítems dañados (sin `idMueble`, `cantidad` nula o menor que 1) y muebles que ya no existen;
   - `subtotalUsd = precioUsd × cantidad` (2 decimales, `HALF_UP`); `subtotalBs = usdABolivares(subtotalUsd)`;
   - `disponible = unidades > 0` (sin revelar la cifra).
6. `totalUsd = Σ subtotalUsd`; `totalBs = usdABolivares(totalUsd)` (se convierte el total en USD, no se suman Bs. redondeados).
7. Responde **200 OK** con `items`, `cantidadItems`, `totalUsd` y `totalBs`.
