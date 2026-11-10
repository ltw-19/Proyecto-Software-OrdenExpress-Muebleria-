# Documentación para Tarjetas de Trello — Backend (Historia: Consultar Mueble)

## Historia: Consultar Mueble / Catálogo y Categorías

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** Consumido por `VistaCatalogo.vue` y `VistaInventario.vue` a través del servicio frontend `muebleServicio.js`.
- **Ruta de acceso en backend:**
  - `GET /api/muebles?busqueda={texto}&categoria={categoria}`
  - `GET /api/muebles/categorias`
  - `GET /api/muebles/{id}`

### Arquitectura (Capas y Endpoints)
- **Flujo de petición:**
  `MuebleControlador` → `MuebleServicio` → `MuebleRepositorio` → `ServicioArchivoJson` (`backend/datos/muebles.json`).
- **Controlador:** [`MuebleControlador.java`](file:///home/juandi46/Projects/Proyecto-Software-OrdenExpress-Muebleria-/backend/src/main/java/com/ordenexpress/orden_express_muebleria/controlador/MuebleControlador.java)
- **Servicio:** [`MuebleServicio.java`](file:///home/juandi46/Projects/Proyecto-Software-OrdenExpress-Muebleria-/backend/src/main/java/com/ordenexpress/orden_express_muebleria/servicio/MuebleServicio.java)
- **Conversión de Moneda:** [`ConversionMonedaServicio.java`](file:///home/juandi46/Projects/Proyecto-Software-OrdenExpress-Muebleria-/backend/src/main/java/com/ordenexpress/orden_express_muebleria/servicio/ConversionMonedaServicio.java) (calcula `precioBs = precioUsd * tasaBCV` con `BigDecimal` y redondeo `HALF_UP`).
- **Seguridad:** Requiere autenticación JWT. La visibilidad de `unidades` (stock) se evalúa con el `UsuarioAutenticado`: si el rol es `ADMIN` o `GESTOR`, se incluye `unidades`; si es `CLIENTE`, `unidades` es `null` (oculto) y solo se expone `disponible`.

### Estructura de Datos
- **Petición `GET /api/muebles?busqueda=sofa&categoria=Sala`:**
  - Cabecera: `Authorization: Bearer <token>`
- **Respuesta (200 OK):**
```json
[
  {
    "id": "2b5ff16b-19ac-4965-9102-1b2b44669696",
    "codigo": "M-001",
    "nombre": "Sofá Alba",
    "categoria": "Sala",
    "descripcion": "Sofá de tres puestos tapizado en lino beige, con estructura y patas de madera de nogal. Una pieza cómoda y atemporal para tu sala.",
    "dimensiones": {
      "ancho": 210,
      "alto": 82,
      "profundidad": 90
    },
    "material": "Lino beige y madera de nogal",
    "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Sofa+Alba",
    "precioUsd": 480.00,
    "precioBs": 19200.00,
    "disponible": true,
    "unidades": 8
  }
]
```
*(Nota: Para usuarios con rol CLIENTE, `"unidades"` es `null`)*.

- **Petición `GET /api/muebles/categorias`:**
- **Respuesta (200 OK):**
```json
[
  "Comedor",
  "Dormitorio",
  "Sala"
]
```

### Algoritmo
1. La petición llega a `MuebleControlador.listar(busqueda, categoria, usuario)` o `listarCategorias()`.
2. `MuebleServicio.consultarMuebles()` lee la lista de muebles desde `muebles.json`.
3. Aplica filtrado opcional por categoría exacta.
4. Aplica filtrado opcional por texto de búsqueda, normalizando acentos/tildes y mayúsculas, comparando sobre: nombre, descripción, material, código y categoría.
5. Para cada mueble coincidente:
   - Determina `disponible = unidades != null && unidades > 0`.
   - Si el usuario autenticado tiene rol `ADMIN` o `GESTOR`, fija `unidadesVisibles = mueble.unidades()`; en caso contrario, fija `unidadesVisibles = null`.
   - Calcula el precio en Bolívares usando `ConversionMonedaServicio.usdABolivares(mueble.precioUsd())`.
6. Retorna la lista resultante mapeada a `MuebleRespuesta` con código HTTP 200 OK.


---

## Historia: Consultar Mueble por ID (Detalle de Mueble)

### Interfaz Humano-Máquina (IHM)
- **Capa cliente:** Consumido por `VistaDetalleMueble.vue` (ruta `/muebles/:id`) mediante `muebleServicio.obtenerPorId(id)`.
- **Ruta de acceso en backend:** `GET /api/muebles/{id}`

### Arquitectura (Capas y Endpoints)
- **Flujo de petición:**
  `MuebleControlador.consultarDetalle(usuario, id)` → `MuebleServicio.consultarDetalle(id, verUnidades)` → `MuebleRepositorio.buscarPorId(id)` → `ServicioArchivoJson` (`datos/muebles.json`).
- **Seguridad:** Requiere autenticación JWT (`ADMIN`, `GESTOR`, `CLIENTE`). La visibilidad de `unidades` sigue la misma regla: solo `ADMIN` y `GESTOR` reciben la cifra exacta; `CLIENTE` solo recibe `disponible`.

### Estructura de Datos
- **Petición `GET /api/muebles/2b5ff16b-19ac-4965-9102-1b2b44669696`:**
  - Cabecera: `Authorization: Bearer <token>`
- **Respuesta (200 OK):** Objeto `MuebleRespuesta` individual (mismo formato que un elemento de `GET /api/muebles`).
- **Error si no existe (404 Not Found):**
`json
{
  "mensaje": "El mueble no existe."
}
`

### Algoritmo
1. `MuebleControlador.consultarDetalle` extrae el `id` de la URL y determina `verUnidades = usuario.rol() != Rol.CLIENTE`.
2. `MuebleServicio.consultarDetalle` valida que el `id` no esté vacío y llama a `MuebleRepositorio.buscarPorId(id.trim())`.
3. Si no existe coincidencia, lanza `ExcepcionNegocio(HttpStatus.NOT_FOUND, "El mueble no existe.")`, que `ManejadorGlobalExcepciones` serializa como `404` JSON.
4. Si existe, convierte el mueble con `aRespuesta(mueble, verUnidades)` calculando `precioBs` con la tasa BCV oficial y ocultando `unidades` al `CLIENTE`.