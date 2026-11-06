# OrdenExpress Mueblería — Frontend (Vue.js 3)

Las reglas compartidas (restricciones, backlog, roles, reglas de negocio, errores) están en el `CLAUDE.md` de la carpeta padre. Este archivo tiene solo lo específico del frontend. Si algo choca con el archivo padre, avisa antes de programar. Este proyecto es **solo la vista**: consume la API REST del backend (carpeta `backend/`).

## Comandos
- Instalar: `npm install`
- Ejecutar: `npm run dev` (puerto **5173**, el único origen que el backend permite por CORS; si cambia, hay que avisar)
- Compilar: `npm run build`
- El backend corre en `http://localhost:8080`. Tómalo de la variable de entorno de Vite `VITE_API_URL`, no lo escribas fijo en los componentes.

## Al cerrar una historia
Bloque para la tarjeta de Trello en `docs/trello/`: **Interfaz Humano-Máquina** (componentes Vue que intervienen), **Arquitectura** (endpoint consumido, método y ruta), **Estructura de datos** (JSON enviado y recibido) y **Algoritmo** (paso a paso de la lógica).

## Stack y convenciones
- **Vue 3** con Vite y **Composition API** (`<script setup>`), Vue Router y **Axios** con `async/await` y `try/catch`. Una sola instancia de Axios con interceptor que agrega `Authorization: Bearer <token>`.
- **CSS3 puro** con Flexbox y Grid, con variables CSS para la paleta. **Sin librerías de UI pesadas** (nada de Bootstrap, Tailwind ni Vuetify) y sin gráficos 3D.
- Componentes y archivos en español y PascalCase (`VistaCatalogo.vue`, `TarjetaMueble.vue`).
- Organización sugerida: `src/vistas/`, `src/componentes/`, `src/servicios/` (un servicio por módulo: `cuentaServicio.js`, `muebleServicio.js`...), `src/router/`, `src/estado/` (sesión) y `src/estilos/`.
- **Mobile-first y responsive** (Chrome, Firefox, Safari, Edge). Cada tabla debe tener una versión usable en móvil.

## Restricciones del frontend
1. **Nunca calcules bolívares ni totales oficiales:** solo muestra lo que entrega el backend con `Intl.NumberFormat('es-VE')`. Excepción: una vista previa informativa, claramente secundaria, al escribir el precio en el formulario de registro.
2. **`localStorage` solo para renderizar la interfaz** (token, nombre, rol). Ocultar un botón no es seguridad: el backend valida todo y puede responder 401/403.
3. **Valida el 100 % de los campos obligatorios antes de enviar** (vacíos, formatos, rangos) y muestra **modales o alertas claras** ante cualquier error HTTP, leyendo siempre `respuesta.data.mensaje`.
4. No hay subida de archivos: la foto del mueble es una **URL** http/https, con vista previa.
5. Un 401 (sesión vencida o inválida) limpia la sesión y redirige al ingreso.

## Pantallas del Sprint 1
Una guarda de ruta redirige al ingreso si no hay sesión y bloquea según el rol.

| Pantalla | Quién | Notas |
|---|---|---|
| Iniciar sesión | pública | Requisito técnico. El texto de acceso incluye al gestor. |
| Crear cuenta | pública | Nombre, apellido, correo, teléfono, contraseña y confirmación; dirección opcional. Tras el 201 lleva al ingreso (no inicia sesión sola). |
| Datos personales | ADMIN, CLIENTE | Correo editable. Se precarga con la respuesta del ingreso. El GESTOR no la ve. |
| Gestión de roles | ADMIN | **Sin mockup todavía.** Lista de cuentas y selector de rol; no puede cambiar el suyo. |
| Catálogo | todos con sesión | Cuadrícula, buscador, filtro de categorías, precio en USD y Bs. "Añadir al carrito" **solo CLIENTE**, con selector de cantidad. |
| Inventario / Registrar mueble | ADMIN, GESTOR | Ven la cifra de unidades. Sin Editar ni Eliminar. |
| Carrito | CLIENTE | Cantidades de **solo lectura** (sin − / +). Eliminar ítem. Datos de entrega precargados y editables. Descargar Pre-orden y Enviar a WhatsApp. |
| Mis pedidos | CLIENTE | **Sin mockup todavía.** Eliminar solo en `PENDIENTE`. |
| Pedidos recibidos | ADMIN, GESTOR | Bandeja global. **Aprobar/Rechazar solo GESTOR**; ADMIN solo lectura. Eliminar solo GESTOR. |
| Detalle de pedido | ADMIN, GESTOR | Sin edición de ítems. |

## Contrato de la API (autenticación: `Authorization: Bearer <token>`)
| Método y ruta | Quién | Cuerpo / respuesta |
|---|---|---|
| `POST /api/cuentas/ingresar` | pública | `{correo, clave}` → `{token, id, nombre, apellido, correo, telefono, direccion, rol}` |
| `POST /api/cuentas/registro` | pública | `{nombre, apellido, correo, clave, telefono, direccion?}` → 201 cuenta |
| `PUT /api/cuentas/yo` | ADMIN, CLIENTE | `{nombre, apellido, correo, telefono, direccion?}` → cuenta |
| `GET /api/cuentas` | ADMIN | lista de cuentas |
| `PUT /api/cuentas/{id}/rol` | ADMIN | `{rol: "ADMIN"\|"GESTOR"\|"CLIENTE"}` |
| `POST /api/muebles` | ADMIN, GESTOR | `{nombre, categoria, descripcion, dimensiones:{ancho,alto,profundidad}, material, urlFoto, precioUsd, unidades}` → 201 mueble |
| `GET /api/muebles?busqueda=&categoria=` | todos | lista (`id, codigo, nombre, categoria, descripcion, dimensiones, material, urlFoto, precioUsd, precioBs, disponible`; `unidades` solo ADMIN/GESTOR) |
| `GET /api/muebles/categorias` | todos | lista de textos para el filtro |

**Aún no existen en el backend:** `GET /api/tasa`, carrito y pedidos (incluida la Pre-orden en PDF). No programes contra ellos hasta que se agreguen aquí.

## Formato
- El precio se envía con **punto decimal** (la interfaz acepta coma y la convierte); máximo 2 decimales y mayor que 0.
- Teléfono: el frontend quita espacios, guiones y paréntesis antes de enviar.
- Dimensiones en **cm**; los mockups las muestran como Ancho × Profundidad × Alto, pero el contrato envía `ancho`, `alto` y `profundidad`.
- El contador del carrito cuenta **unidades**.
- Estados: `PENDIENTE` amarillo, `APROBADO` verde, `RECHAZADO` rojo.
- Nota de tasa: no uses el texto fijo "1 USD = Bs. 50,00. No es una cotización real"; muestra la tasa real y su fecha desde `GET /api/tasa` cuando exista.

## Diseño UI/UX
- Fondo `#FAFAFA` / `#FFFFFF`. Acentos nogal `#8D6E63` y `#5D4037`. Texto `#212529`. Textos pequeños en gris con contraste 4.5:1.
- **Verde `#25D366` reservado EXCLUSIVAMENTE para el botón "Enviar Pedido a WhatsApp".**
- Fuente `Inter` (o `Poppins`), tarjetas con `border-radius: 12px`, sombras suaves, maquetación con Grid y Flexbox.
- Los 13 mockups son la referencia visual (barra lateral oscura para ADMIN/GESTOR es un desvío aceptable). Cuando un mockup contradiga estos archivos (botones − / +, subida de foto, edición de ítems del pedido, nota de tasa fija, botones de pedido para el ADMIN), **mandan los archivos**.

## Progreso del Frontend
- [x] **Esqueleto base:** Vite (puerto 5173), Vue Router con guardas por rol y autenticación, cliente Axios con interceptores JWT y gestión de sesión reactiva con localStorage.
- [x] **Iniciar sesión:** Vista VistaIngreso.vue con validación y redirección.
- [x] **Crear cuenta:** Vista VistaRegistro.vue con validación 100% en cliente, limpieza de teléfono y redirección tras 201.
- [x] **Datos personales:** Vista VistaDatosPersonales.vue (ADMIN y CLIENTE, GESTOR bloqueado).
- [x] **Gestión de roles:** Vista VistaGestionRoles.vue (ADMIN, sin poder cambiar el propio, versión de tabla y tarjetas para móvil).

## Pendiente (Historias de tu compañero y Sprint 1 restante)
- [ ] Catálogo de muebles (VistaCatalogo.vue, búsqueda y categorías).
- [ ] Inventario / Registrar mueble (ADMIN, GESTOR).
- [ ] Carrito (CLIENTE) - *Pendiente de endpoint en backend*.
- [ ] Mis pedidos (CLIENTE) - *Pendiente de endpoint en backend*.
- [ ] Pedidos recibidos y detalle (ADMIN, GESTOR) - *Pendiente de endpoint en backend*.
