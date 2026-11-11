# Documentación para Tarjetas de Trello — Frontend (Módulo de Carrito)

## 7. Historia: Añadir ítem, Consultar y Eliminar del Carrito (CLIENTE) — Sprint 1

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaCatalogo.vue` y `TarjetaMueble.vue`:
    - El CLIENTE selecciona la cantidad deseada (mínimo 1) y pulsa "Añadir al carrito".
    - Despacha `POST /api/carrito/items`.
    - Actualiza inmediatamente el contador de unidades reactivo en la barra de navegación (`carritoEstado.conteoUnidades`).
    - Despliega alertas claras de éxito o de conflicto (409 "Stock insuficiente" o "Este mueble ya está en tu carrito. Elimínalo para añadirlo con otra cantidad.").
  - `BarraNavegacion.vue`:
    - Enlace reactivo a `🛒 Carrito` con insignia numérica que cuenta las unidades totales acumuladas (`badge-carrito`).
  - `VistaCarrito.vue`:
    - **Tabla para pantallas de escritorio y tarjetas mobile-first para pantallas móviles**, cumpliendo la regla de diseño responsivo.
    - Cada fila/tarjeta expone: foto miniatura, nombre del mueble, código único (`M-001`), precios bimonetarios formateados con `Intl.NumberFormat('es-VE')`, cantidad de **solo lectura** (cumpliendo estrictamente la prohibición de botones `− / +` para el Sprint 1) y subtotales en USD y Bs. calculados exclusivamente por el backend.
    - Indicador de mueble "Agotado" si `disponible` es falso.
    - Botón de eliminación individual con modal de confirmación accesible.
    - Estado vacío ("Tu carrito está vacío") con botón de redirección al catálogo.
    - Resumen de totales oficiales en USD y en Bs.
    - Formulario de datos de entrega para procesar el pedido.
- **Ruta frontend:** `/carrito` (protegida por guardas de navegación, exclusiva para rol `CLIENTE`).

### Arquitectura (Capas y Endpoints)
- **Capa y servicios:**
  - `VistaCarrito.vue` → `carritoServicio.consultarCarrito()`, `carritoServicio.eliminarItem()` → `api.js` (Axios con interceptor JWT).
  - `VistaCatalogo.vue` → `carritoServicio.agregarItem()` → `carritoEstado.js`.
- **Endpoints consumidos:**
  - `GET /api/carrito` (200 OK con `CarritoRespuesta`).
  - `POST /api/carrito/items` (201 Created con el carrito actualizado).
  - `DELETE /api/carrito/items/{idMueble}` (200 OK con el carrito actualizado y totales recalculados).

### Estructura de Datos
- **Petición para añadir ítem (`POST /api/carrito/items`):**
```json
{
  "idMueble": "2b5ff16b-19ac-4965-9102-1b2b44669696",
  "cantidad": 2
}
```
- **JSON recibido del backend (`CarritoRespuesta` - 200 OK / 201 Created):**
```json
{
  "items": [
    {
      "idMueble": "2b5ff16b-19ac-4965-9102-1b2b44669696",
      "codigo": "M-001",
      "nombre": "Sofá Alba",
      "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Sofa+Alba",
      "precioUsd": 480.00,
      "precioBs": 19200.00,
      "cantidad": 2,
      "subtotalUsd": 960.00,
      "subtotalBs": 38400.00,
      "disponible": true
    }
  ],
  "cantidadItems": 1,
  "totalUsd": 960.00,
  "totalBs": 38400.00
}
```

### Algoritmo
1. Al montar `VistaCarrito.vue`, se invoca `carritoServicio.consultarCarrito()` pasando el token JWT en las cabeceras.
2. Si el carrito contiene ítems:
   - Se puebla el estado `items`, `totalUsd` y `totalBs`.
   - Se renderizan las líneas de la tabla/tarjetas formateando montos con `Intl.NumberFormat('es-VE')`.
   - No se ofrecen controles de incremento o decremento de cantidades (regla Sprint 1).
3. **Eliminar ítem:**
   - El cliente presiona el ícono del basurero en un producto.
   - Se abre el modal de confirmación con el nombre y código del mueble.
   - Al confirmar, se envía `DELETE /api/carrito/items/{idMueble}`.
   - Con la respuesta 200 OK, se actualizan inmediatamente los ítems y totales devueltos por el servidor, sincronizando también la insignia del navbar en `carritoEstado`.
