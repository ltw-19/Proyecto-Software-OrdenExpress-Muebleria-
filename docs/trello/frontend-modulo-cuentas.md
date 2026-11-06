# Documentación para Tarjetas de Trello — Frontend (Módulo de Cuentas y Roles)

## 1. Historia: Iniciar Sesión (Requisito Técnico)

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaIngreso.vue`: Formulario con campos de correo electrónico y contraseña, textos explicativos para los tres roles (Cliente, Gestor de Ventas, Administrador), enlaces a registro y mensajes de validación y alerta.
  - `AlertaMensaje.vue`: Despliegue de errores HTTP (`respuesta.data.mensaje`) o aviso de cuenta creada exitosamente.
  - `BarraNavegacion.vue`: Actualización reactiva al cambiar el estado de sesión mostrando el nombre y rol del usuario.
- **Ruta frontend:** `/ingreso`

### Arquitectura
- **Capa y servicios:** `VistaIngreso.vue` → `cuentaServicio.ingresar()` → `api.js` (instancia de Axios con base URL desde `VITE_API_URL`).
- **Endpoint consumido:** `POST /api/cuentas/ingresar` (público).

### Estructura de Datos
- **JSON enviado (IngresoSolicitud):**
```json
{
  "correo": "admin@ordenexpress.com",
  "clave": "Admin2026*"
}
```
- **JSON recibido (IngresoRespuesta - 200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": "e6f43f1c-7b9e-4c3d-82a1-0987654321ab",
  "nombre": "Administrador",
  "apellido": "Sistema",
  "correo": "admin@ordenexpress.com",
  "telefono": "04121234567",
  "direccion": "Sede Principal",
  "rol": "ADMIN"
}
```

### Algoritmo
1. El usuario introduce correo y contraseña y presiona "Entrar".
2. **Validación en cliente:** Se verifica que el correo no esté vacío y cumpla expresión regular de formato email; se verifica que la contraseña no esté vacía.
3. Si la validación falla, se marcan los campos con bordes rojos y mensajes de error sin realizar la petición HTTP.
4. Si es válida, se activa el estado de carga (`cargando = true`) y se envía `POST /api/cuentas/ingresar`.
5. **En caso de éxito (200):** Se guarda el token JWT y los datos mínimos de perfil en el estado reactivo (`sesion.js`) y en `localStorage`, y se redirige al catálogo (`/catalogo`).
6. **En caso de error:** Se captura en el bloque `catch`, se extrae `error.response.data.mensaje` y se renderiza en `AlertaMensaje`.

---

## 2. Historia: Crear Cuenta (Registro de Cliente)

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaRegistro.vue`: Formulario responsive en cuadrícula con campos: nombre, apellido, correo, teléfono, contraseña, confirmar contraseña y dirección opcional.
  - `AlertaMensaje.vue`: Visualización de errores de validación del servidor (ej. correo duplicado 409).
- **Ruta frontend:** `/registro`

### Arquitectura
- **Capa y servicios:** `VistaRegistro.vue` → `cuentaServicio.registrar()` → `api.js`.
- **Endpoint consumido:** `POST /api/cuentas/registro` (público).

### Estructura de Datos
- **JSON enviado (RegistroSolicitud):**
```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "correo": "juan.perez@correo.com",
  "clave": "ClaveSegura123*",
  "telefono": "04121234567",
  "direccion": "Av. Bolívar, Casa #42"
}
```
- **JSON recibido (CuentaRespuesta - 201 Created):**
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "nombre": "Juan",
  "apellido": "Pérez",
  "correo": "juan.perez@correo.com",
  "telefono": "04121234567",
  "direccion": "Av. Bolívar, Casa #42",
  "rol": "CLIENTE"
}
```

### Algoritmo
1. El usuario completa los datos en el formulario.
2. **Validación en cliente:**
   - Nombre y apellido obligatorios y no vacíos.
   - Formato válido de correo electrónico.
   - Teléfono: se eliminan espacios, guiones y paréntesis mediante expresión regular (`replace(/[\s\-\(\)]/g, '')`) y se valida que tenga entre 10 y 15 dígitos numéricos (`^\+?[0-9]{10,15}$`).
   - Contraseña de longitud entre 8 y 64 caracteres.
   - Confirmación de contraseña idéntica a la contraseña.
   - Dirección opcional de máximo 200 caracteres.
3. Se envía `POST /api/cuentas/registro`.
4. **Al recibir 201:** Cumpliendo estrictamente la regla de negocio, **no se inicia sesión automáticamente**; el router redirige a `/ingreso?registrado=true`, mostrando una alerta verde indicando que el registro fue exitoso y que el usuario puede ingresar.
5. **En caso de error:** Se captura el mensaje del backend (ej: "El correo ya se encuentra registrado") y se muestra en pantalla.

---

## 3. Historia: Modificar Datos Personales

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaDatosPersonales.vue`: Vista protegida accesible solo para roles `ADMIN` y `CLIENTE` (el `GESTOR` está excluido según regla de negocio). Precarga los datos del usuario en sesión.
  - `AlertaMensaje.vue`: Confirmación de actualización o notificación de error.
- **Ruta frontend:** `/perfil`

### Arquitectura
- **Capa y servicios:** `VistaDatosPersonales.vue` → `cuentaServicio.modificarMisDatos()` → `api.js` con encabezado `Authorization: Bearer <token>`.
- **Endpoint consumido:** `PUT /api/cuentas/yo`.

### Estructura de Datos
- **JSON enviado (ModificacionCuentaSolicitud):**
```json
{
  "nombre": "Juan Carlos",
  "apellido": "Pérez",
  "correo": "nuevo.correo@correo.com",
  "telefono": "04149876543",
  "direccion": "Urb. Los Rosales, Calle 3"
}
```
- **JSON recibido (CuentaRespuesta - 200 OK):**
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "nombre": "Juan Carlos",
  "apellido": "Pérez",
  "correo": "nuevo.correo@correo.com",
  "telefono": "04149876543",
  "direccion": "Urb. Los Rosales, Calle 3",
  "rol": "CLIENTE"
}
```

### Algoritmo
1. En `onMounted`, se precargan los campos a partir de `sesion.usuario`.
2. El usuario modifica nombre, apellido, correo (editable), teléfono o dirección.
3. Se validan los campos en el cliente con las mismas restricciones de formato.
4. Se despacha `PUT /api/cuentas/yo` con el token JWT en el encabezado.
5. Al recibir 200 OK, se actualizan los datos en `sesion` y en `localStorage` (sin alterar el token ni el rol).
6. Se despliega una alerta de confirmación ("Tus datos personales se han actualizado correctamente").

---

## 4. Historia: Gestión de Roles

### Interfaz Humano-Máquina (IHM)
- **Componentes Vue involucrados:**
  - `VistaGestionRoles.vue`: Vista exclusiva del Administrador (`ADMIN`). Contiene versión de tabla para pantallas grandes y cuadrícula de tarjetas para dispositivos móviles (mobile-first).
  - Selector desplegable de rol (`CLIENTE`, `GESTOR`, `ADMIN`) por cada usuario.
  - Deshabilitación obligatoria del selector para la propia cuenta del administrador logueado, con indicador visual `(Tu cuenta)`.
- **Ruta frontend:** `/roles`

### Arquitectura
- **Capa y servicios:** `VistaGestionRoles.vue` → `cuentaServicio.listarCuentas()` y `cuentaServicio.cambiarRol()`.
- **Endpoints consumidos:**
  - `GET /api/cuentas` (listar)
  - `PUT /api/cuentas/{id}/rol` (cambio de rol)

### Estructura de Datos
- **JSON recibido en listado (GET /api/cuentas):**
```json
[
  {
    "id": "admin-id-1",
    "nombre": "Administrador",
    "apellido": "Principal",
    "correo": "admin@ordenexpress.com",
    "telefono": "04120000001",
    "direccion": "Sede",
    "rol": "ADMIN"
  },
  {
    "id": "user-id-2",
    "nombre": "Carlos",
    "apellido": "Ventas",
    "correo": "carlos@correo.com",
    "telefono": "04120000002",
    "direccion": "",
    "rol": "CLIENTE"
  }
]
```
- **JSON enviado en cambio de rol (CambioRolSolicitud):**
```json
{
  "rol": "GESTOR"
}
```
- **JSON recibido (CuentaRespuesta - 200 OK):** Objeto de la cuenta con el rol actualizado.

### Algoritmo
1. Al cargar la vista, se llama a `GET /api/cuentas` para obtener la lista de usuarios.
2. La interfaz detecta si el `cuenta.id === sesion.usuario.id`:
   - Si es la propia cuenta, deshabilita el selector de rol e incluye la etiqueta `(Tu cuenta)`.
3. Si el administrador selecciona un nuevo rol en otra cuenta:
   - Se activa el indicador de carga en dicha fila.
   - Se envía `PUT /api/cuentas/{id}/rol` con `{ rol: nuevoRol }`.
   - Al responder 200, se actualiza el rol en la lista y se muestra alerta de éxito.
   - Si la API responde error (403, 404, 400), se muestra el mensaje de error y se refresca la lista para revertir cambios.