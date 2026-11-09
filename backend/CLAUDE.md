# OrdenExpress Mueblería — Backend (Spring Boot)

Las reglas compartidas (restricciones, backlog, roles, reglas de negocio, errores) están en el `CLAUDE.md` de la carpeta padre. Este archivo tiene solo lo específico del backend. Si algo choca con el archivo padre, avisa antes de programar.

## Comandos (Windows)
- Ejecutar: `.\mvnw.cmd spring-boot:run` (puerto 8080)
- Pruebas: `.\mvnw.cmd test`
- Datos limpios: borrar la carpeta `datos/` y reiniciar (se regenera la semilla).

## Al cerrar una historia
(1) Colección de **Postman** en `docs/pruebas/` (JSON, solo caracteres ASCII, con scripts de verificación; Thunder Client no importa bien los archivos). (2) Bloque para Trello en `docs/trello/`: Interfaz Humano-Máquina, Arquitectura (endpoint y capas), Estructura de datos (nodo JSON exacto) y Algoritmo.

## Stack
Java 25, Maven, **Spring Boot 4.1.2** (Spring Framework 7, Spring Security 7), JJWT 0.12.6 con **`jjwt-gson`** (no `jjwt-jackson`), OpenPDF 1.3.43 (paquete `com.lowagie`) para el PDF.
- **Jackson 3:** los imports son `tools.jackson.*` (por ejemplo `tools.jackson.databind.json.JsonMapper`). Solo las anotaciones siguen en `com.fasterxml.jackson.annotation`. Nunca uses `com.fasterxml.jackson.databind`.
- Entidades y DTO como `record`. Seguridad con DSL de lambdas (sin `.and()`).
- Paquete raíz `com.ordenexpress.orden_express_muebleria`: `configuracion`, `controlador`, `servicio`, `repositorio`, `modelo`, `dto`, `seguridad`, `excepcion`, `util`.
- Prohibido MySQL, PostgreSQL, H2, MongoDB, JPA e Hibernate.

## Convenciones de código
- Nombres de clases en PascalCase y en español (`ServicioArchivoJson`, `CuentaServicio`).
- Capas: `Controlador` → `Servicio` → `Repositorio` → `ServicioArchivoJson`. El repositorio solo lee y guarda; las reglas de negocio van en el servicio.
- Todo acceso a `datos/*.json` pasa por `ServicioArchivoJson` (candado por archivo, escritura atómica con archivo temporal). Las operaciones "leer y luego escribir" van siempre dentro de `modificarLista`. Si tocan dos archivos (aprobar un pedido y descontar stock), ordena los pasos y compensa si el segundo falla.
- `ServicioArchivoJson` nunca devuelve lista vacía si falla la lectura (lanza `ExcepcionAlmacenamiento`) para no sobrescribir datos reales.
- Validación con `@Valid` y anotaciones en los `record` DTO, con mensajes en español. Errores mediante `ExcepcionNegocio` y `ManejadorGlobalExcepciones`.
- `FiltroAutenticacionJwt` no lleva `@Component` (se instancia en `ConfiguracionSeguridad`).
- Dinero con `BigDecimal`. No uses `stripTrailingZeros` (produce notación científica en el JSON). Para totales, convierte el total en USD, no sumes montos ya redondeados.
- Si falta una propiedad en `application.properties`, la app no arranca: agrégala ahí cuando crees una.

## Seguridad
- Contraseñas con BCrypt (solo procesa 72 bytes: clave de 8 a 64 caracteres).
- JWT HS256, 8 horas, `Authorization: Bearer`, secreto `app.jwt.secreto` (≥ 32 caracteres; variable de entorno `JWT_SECRETO`).
- El filtro **relee el rol desde `usuarios.json` en cada petición**. El id de quien actúa sale SIEMPRE del token, nunca del cuerpo ni de la URL. Permisos con `@PreAuthorize`.
- Rutas públicas (solo POST): `/api/cuentas/ingresar` y `/api/cuentas/registro`. Todo lo demás exige token.
- CORS solo `http://localhost:5173`.
- Cada pedido guarda `timestamp` inmutable e `idUsuario`.

## Reglas que solo aplican aquí
- **Tasa BCV automática con respaldo:** consultar una fuente al arrancar y periódicamente, guardarla en memoria, validar que sea positiva y razonable y, si falla, usar `tasa.bcv` de `application.properties`. La fuente no es oficial: documentarla en Trello.
- `PUT /api/cuentas/yo` solo para ADMIN y CLIENTE. El ingreso devuelve también apellido, teléfono y dirección (para precargar formularios).
- Número de pedido: `OE-0001` = mayor existente + 1, además del id UUID.
- Eliminar ítem del carrito recalcula totales y no deja valores `null` en `carritos.json`.

## Datos iniciales
`SemillaServicio` crea el ADMIN y el Gestor si `usuarios.json` está vacío (credenciales en `application.properties`, solo desarrollo; cambiarlas antes de entregar) y, en un sistema nuevo, 6 muebles de ejemplo. Es un requisito técnico, no una historia.

## Endpoints implementados
`POST /api/cuentas/ingresar`, `POST /api/cuentas/registro`, `PUT /api/cuentas/yo`, `GET /api/cuentas`, `PUT /api/cuentas/{id}/rol`, `POST /api/muebles`, `GET /api/muebles`, `GET /api/muebles/categorias`, `POST /api/carrito/items`, `GET /api/carrito`, `DELETE /api/carrito/items/{idMueble}` (CLIENTE; 200 con el carrito y totales recalculados; 404 si no está en su carrito; no toca stock), `POST /api/pedidos` (CLIENTE; cuerpo `{nombre, apellido, telefono, direccion}`; ítems del carrito, que se vacía; 201 con `enlaceWhatsapp`), `GET /api/pedidos/{id}/preorden` (CLIENTE dueño; PDF), `GET /api/pedidos` (todos; CLIENTE los suyos, ADMIN y GESTOR todos; más reciente primero), `GET /api/pedidos/{id}` (todos; CLIENTE solo el suyo), `PUT /api/pedidos/{id}/aprobar` (GESTOR; solo desde PENDIENTE; descuenta stock de todos los ítems o de ninguno, con compensación si falla guardar el pedido), `PUT /api/pedidos/{id}/rechazar` (GESTOR; solo desde PENDIENTE; no toca stock), `DELETE /api/pedidos/{id}` (CLIENTE solo los suyos en PENDIENTE, si no 403; GESTOR cualquiera; ADMIN no; 204; no devuelve stock).

## Pendiente
Tasa automática. *Actualiza esta sección y la lista de endpoints al cerrar cada historia.*
