# Documentación para Tarjetas de Trello — Frontend (Módulo de Muebles)

## 5. Historia: Consultar Mueble / Catálogo de Muebles

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaCatalogo.vue`: Vista principal del catálogo con barra de búsqueda reactiva por texto (nombre, material, descripción con debounce de 350 ms), filtro dinámico por selector de categoría, contador de resultados, botón para limpiar filtros, recarga de catálogo y cuadrícula responsive mobile-first.
  - `TarjetaMueble.vue`: Tarjeta individual para cada mueble con:
    - Imagen de producto con control de error y reemplazo SVG con marca "Galería de Casa".
    - Insignia con código único de pieza (`M-001`, `M-002`...).
    - Insignia de categoría.
    - Indicador de disponibilidad y stock condicional:
      - **CLIENTE:** Visualiza únicamente "Disponible" o "Agotado", garantizando que **nunca se le revele la cifra de unidades numéricas**.
      - **ADMIN / GESTOR:** Visualiza la cantidad exacta de stock disponible en unidades (`Stock: X uds.`).
    - Nombre, material, dimensiones en cm (`Ancho × Profundidad × Alto`) y descripción.
    - Precios bimonetarios: precio en USD destacado y precio en Bolívares (`precioBs`) formateado mediante `Intl.NumberFormat('es-VE')`, sin que el frontend realice cálculos matemáticos arbitrarios de divisas ni tasas fijas inventadas.
    - Sección de acciones:
      - **CLIENTE:** Selector numérico de cantidad (mínimo 1) y botón "Añadir al carrito" (con paleta de acentos nogal, respetando estrictamente la exclusividad de `#25D366` para WhatsApp).
      - **ADMIN / GESTOR:** Modo de solo lectura de catálogo con referencia técnica y acceso al inventario.
  - `AlertaMensaje.vue`: Visualización de errores HTTP o notificaciones informativas al interactuar con el catálogo.
- **Ruta frontend:** `/catalogo` (requiere autenticación, accesible a todos los roles autenticados: `CLIENTE`, `ADMIN`, `GESTOR`).

### Arquitectura
- **Capa y servicios:**
  - `VistaCatalogo.vue` y `TarjetaMueble.vue` → `muebleServicio.obtenerMuebles()` y `muebleServicio.obtenerCategorias()` → `api.js` (instancia de Axios con interceptores JWT y `VITE_API_URL`).
- **Endpoints consumidos:**
  - `GET /api/muebles?busqueda=&categoria=` (requiere token Bearer).
  - `GET /api/muebles/categorias` (requiere token Bearer).

### Estructura de Datos
- **Parámetros de consulta (Query Params):**
```
GET /api/muebles?busqueda=sofa&categoria=Sala
```
- **JSON recibido en listado de muebles (`GET /api/muebles` - 200 OK):**
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
    "precioBs": 24000.00,
    "disponible": true,
    "unidades": 8
  }
]
```
- **JSON recibido en categorías (`GET /api/muebles/categorias` - 200 OK):**
```json
[
  "Sala",
  "Dormitorio",
  "Comedor"
]
```

### Algoritmo
1. Al montarse la vista (`onMounted`), se ejecutan concurrentemente `cargarCategorias()` y `cargarMuebles()`.
2. `cargarMuebles()` invoca `muebleServicio.obtenerMuebles()` pasando los parámetros reactivos `busqueda` (texto limpio) y `categoria` (omitiendo el parámetro si está en 'TODAS').
3. El interceptor de Axios incluye automáticamente la cabecera `Authorization: Bearer <token>`.
4. Si la consulta tiene éxito (200 OK):
   - Se puebla el arreglo reactivo `muebles`.
   - Se renderizan los componentes `TarjetaMueble` en una cuadrícula CSS Grid responsive.
   - En cada tarjeta, los precios se formatean usando `Intl.NumberFormat('es-VE')`.
   - Según el rol en `sesion`: el cliente ve disponibilidad y control de compra; el gestor y administrador ven la cifra exacta de stock.
5. Si no hay resultados o la lista está vacía, se despliega el estado vacío accesible con opción de reestablecer los filtros.
6. Si ocurre un error HTTP, se captura en el bloque `catch` y `extraerMensajeError(error)` extrae `error.response.data.mensaje` o el mensaje de falla de conexión, mostrándolo en `AlertaMensaje`.

---

## 5b. Historia: Consultar Mueble (Página Dedicada de Detalle)

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `TarjetaMueble.vue`: Enlaces `<router-link :to="/muebles/${mueble.id}">` sobre la imagen del producto, el título y el enlace `"Ver descripción y foto completa →"`.
  - `VistaDetalleMueble.vue`: Página dedicada que presenta:
    - Barra superior con botón `"← Volver al Catálogo"` y miga de pan (`Catálogo / Categoría / Nombre`).
    - Imagen en gran formato con insignia de código y reemplazo SVG si falla la carga.
    - Descripción completa sin recorte de líneas, material y dimensiones (`Ancho × Profundidad × Alto cm`).
    - Bloque de precios bimonetarios en USD y Bolívares (tasa BCV oficial) formateados con `Intl.NumberFormat('es-VE')`.
    - **CLIENTE:** Selector de cantidad validado (entero >= 1), botón `"Añadir al carrito"` y acceso rápido `"🛒 Ir a mi Carrito"` tras añadir. Nunca visualiza el número exacto de stock (solo `"Disponible para entrega"` o `"Temporalmente agotado"`).
    - **ADMIN / GESTOR:** Visualizan las unidades exactas disponibles en inventario y un acceso directo `"📦 Ir al Inventario"`, en modo de solo consulta.
  - `AlertaMensaje.vue`: Notificaciones de éxito al añadir al carrito o errores HTTP (ej. 409 stock insuficiente o mueble no encontrado).
- **Ruta frontend:** `/muebles/:id` (requiere autenticación; accesible para `CLIENTE`, `ADMIN` y `GESTOR`).

### Arquitectura
- **Capa y servicios:** `VistaDetalleMueble.vue` → `muebleServicio.obtenerPorId(id)` y `carritoServicio.agregarItem(idMueble, cantidad)` → `api.js` (Axios con token JWT).
- **Endpoints consumidos:**
  - `GET /api/muebles/{id}` (consulta directa del detalle del mueble por su ID; `404` si no existe).
  - `POST /api/carrito/items` (cuando un `CLIENTE` añade unidades desde la ficha).

### Algoritmo
1. El usuario hace clic en la foto, título o enlace de detalle en cualquier tarjeta de `VistaCatalogo.vue`.
2. Vue Router navega a `/muebles/:id`, activa la guarda de autenticación y desplaza el scroll al inicio de la página.
3. Al montarse `VistaDetalleMueble.vue` (o cambiar el parámetro `id` de la ruta), invoca `muebleServicio.obtenerPorId(route.params.id)`, que consulta directamente `GET /api/muebles/{id}`.
4. Si el servidor responde `404 Not Found` o falla la petición, muestra la alerta con `respuesta.data.mensaje` y el estado de `"Mueble no encontrado"` con botón de regreso al catálogo.
5. Si se encuentra, renderiza la ficha completa adaptando disponibilidad y controles según el rol en `sesion`.
6. Si un `CLIENTE` presiona `"Añadir al carrito"`, se valida que la cantidad sea un entero positivo dentro del rango permitido antes de enviar `POST /api/carrito/items`, actualizando el contador reactivo de `BarraNavegacion.vue`.

---

## 6. Historia: Registrar Mueble e Inventario

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaInventario.vue`:
    - Vista reservada para roles `ADMIN` y `GESTOR`.
    - Tabla completa en pantalla de escritorio y lista de tarjetas mobile-first en móviles con visualización de: código, miniatura de foto, nombre, categoría, dimensiones (Ancho × Profundidad × Alto), material, precio USD, precio Bs. y stock en unidades.
    - Barra de búsqueda interna en inventario.
    - Modal de Registro de Mueble con formulario completo y vista previa inmediata en tiempo real de la imagen según la URL ingresada.
    - Sin botones de Editar ni Eliminar (reservados para Sprint 2).
  - `AlertaMensaje.vue`: Despliegue de confirmación tras registrar exitosamente (código asignado por servidor) o errores de validación y de red.
- **Ruta frontend:** `/inventario` (protegida por guardas de navegación, `roles: ['ADMIN', 'GESTOR']`).

### Arquitectura
- **Capa y servicios:**
  - `VistaInventario.vue` → `muebleServicio.registrarMueble()` y `muebleServicio.obtenerMuebles()` → `api.js`.
- **Endpoints consumidos:**
  - `GET /api/muebles` (listar inventario).
  - `POST /api/muebles` (crear mueble en el inventario).

### Estructura de Datos
- **JSON enviado en registro (`POST /api/muebles`):**
```json
{
  "nombre": "Mesa de Centro Olmo",
  "categoria": "Sala",
  "descripcion": "Mesa de centro baja de líneas suaves, con patas redondeadas y acabado cálido.",
  "dimensiones": {
    "ancho": 110.0,
    "alto": 40.0,
    "profundidad": 60.0
  },
  "material": "Madera de olmo",
  "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Mesa+de+centro+Olmo",
  "precioUsd": 220.00,
  "unidades": 6
}
```
- **JSON recibido (`POST /api/muebles` - 201 Created):**
```json
{
  "id": "9dfc2c4f-673a-4c88-8a65-8fd265cab053",
  "codigo": "M-003",
  "nombre": "Mesa de Centro Olmo",
  "categoria": "Sala",
  "descripcion": "Mesa de centro baja de líneas suaves, con patas redondeadas y acabado cálido.",
  "dimensiones": {
    "ancho": 110.0,
    "alto": 40.0,
    "profundidad": 60.0
  },
  "material": "Madera de olmo",
  "urlFoto": "https://placehold.co/600x400/8D6E63/FFFFFF/png?text=Mesa+de+centro+Olmo",
  "precioUsd": 220.00,
  "precioBs": 11000.00,
  "unidades": 6
}
```

### Algoritmo
1. El Administrador o Gestor abre el modal de registro y completa los datos.
2. **Validación en cliente (100% de los campos obligatorios):**
   - Nombre obligatorio no vacío.
   - Categoría seleccionada o texto de nueva categoría.
   - Descripción obligatoria con longitud máxima de 500 caracteres.
   - Dimensiones numéricas mayores a 0 en cm.
   - Material obligatorio.
   - URL de foto que cumpla expresión regular de protocolo `http://` o `https://`.
   - Precio en USD mayor a 0, admitiendo coma o punto decimal y máximo 2 decimales.
   - Unidades en stock como número entero entre 0 y 100.000.
3. Se despacha `POST /api/muebles` con el cuerpo JSON estructurado.
4. **Al responder 201 Created:**
   - Se cierra el modal de registro.
   - Se muestra una alerta verde con el nombre del mueble y el código autogenerado por el backend (`M-00X`).
   - Se recargan la lista de inventario y las categorías actualizadas.
5. **En caso de error:** Se extrae `respuesta.data.mensaje` y se despliega en pantalla manteniendo los datos ingresados en el formulario para corrección.
