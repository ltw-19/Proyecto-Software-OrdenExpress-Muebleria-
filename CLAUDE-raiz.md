# OrdenExpress Mueblería — Reglas compartidas (backend + frontend)

Proyecto universitario de Ingeniería de Software para la mueblería "Galería de Casa". Es **un solo proyecto** con dos carpetas: `backend/` (Spring Boot) y `frontend/` (Vue.js 3). Este archivo tiene lo que aplica a ambas; cada carpeta tiene su propio `CLAUDE.md` con lo específico.

**La profesora penaliza cualquier desviación del alcance o de las restricciones**: ante la duda, pregunta antes de programar.

## Cómo debes trabajar
- Responde en **español**. Al terminar, explica de forma sencilla, breve y paso a paso qué hiciste.
- Si lo que te piden contradice el backlog, una restricción o una decisión de estos archivos (sobre todo si toca el Sprint 2), **avísalo ANTES de programar** y propón una alternativa que cumpla. Señala los problemas sin halagos.
- No agregues funcionalidades fuera del backlog ni adelantes historias del Sprint 2.
- Antes de modificar un archivo, léelo. Respeta la estructura y el estilo existentes. No inventes clases ni endpoints: revisa el código.
- Trabaja solo en la carpeta de la que te hablan. No modifiques el otro proyecto: si hace falta un cambio allá (un endpoint nuevo, un campo), dilo y deja que el usuario lo decida.

## Restricciones inquebrantables
1. **Persistencia solo en archivos JSON** (los maneja el backend en `backend/datos/`). Prohibido cualquier base de datos.
2. **Sin pasarelas de pago.** El flujo termina con la Pre-orden en PDF y la redirección a WhatsApp con mensaje pre-estructurado.
3. **Todo en español:** interfaz, mensajes, nombres de clases, variables, endpoints y comentarios.
4. **Cero historias extra:** nada de listas de deseos, cupones, chatbots, gráficas ni recuperación de clave por correo.

## Roles y backlog
Roles: `ADMIN`, `GESTOR` (Gestor de Ventas), `CLIENTE`.

**Sprint 1 (alcance actual)**
- Mueble: *Registrar* (ADMIN, GESTOR), *Consultar* (TODOS; búsqueda, categorías y precio bimonetario).
- Carrito: *Añadir ítem*, *Consultar* y *Eliminar ítem* (CLIENTE).
- Pedido: *Registrar* (CLIENTE), *Consultar* (TODOS: el cliente ve los suyos; ADMIN y GESTOR la bandeja global), *Eliminar* (CLIENTE, GESTOR).
- Cuenta: *Crear* (CLIENTE), *Modificar* (ADMIN, CLIENTE; el ADMIN además cambia roles). Iniciar sesión es requisito técnico, no historia.

**Sprint 2 (NO hacer todavía):** Editar/Eliminar Mueble, Modificar ítem Carrito (botones − / +), Editar Pedido, reseñas (añadir, consultar, modificar, eliminar, moderar), Cambiar Contraseña, Consultar Cuenta.

## Reglas de negocio (valen para los dos lados)
**Bimonetario:** solo se guarda `precioUsd`. Los bolívares (`precioBs`) y los totales los calcula **el backend** (tasa BCV, `BigDecimal`, 2 decimales, `HALF_UP`); **el frontend nunca los calcula**, solo los muestra. El pedido **congela** el precio unitario y la tasa del momento de registrarse. La tasa es automática con respaldo y se expone en `GET /api/tasa` → `{tasa, fecha, origen}` (*pendiente de implementar*).

**Integridad:** el backend nunca confía en montos que mande el navegador; calcula todo con los precios de `muebles.json`.

**Cuenta:** `nombre` y `apellido` separados y obligatorios; `correo` único y editable; `telefono` obligatorio (`^\+?[0-9]{10,15}$`, sin espacios ni guiones); `direccion` opcional (≤ 200); contraseña de 8 a 64 caracteres. Toda cuenta nueva es `CLIENTE`. El GESTOR no tiene *Modificar Cuenta*. El ADMIN lista cuentas y cambia el rol de otras, **nunca el suyo**.

**Mueble:** `codigo` (`M-001`...) e `id` (UUID) los genera el servidor. Dimensiones en **cm**. `urlFoto` es una URL http/https a una imagen directa (no hay subida de archivos). `descripcion` obligatoria (≤ 500). `unidades` de 0 a 100000.

**Stock (aprobado):** `unidades` se fija al registrar. Añadir al carrito y registrar pedido rechazan cantidad mayor al stock (**409 "Stock insuficiente"**). Se **descuenta cuando el GESTOR aprueba** (todo el pedido o nada). Rechazar o eliminar **no devuelve** stock. Solo ADMIN y GESTOR ven la cifra `unidades`; el CLIENTE ve solo `disponible` / "Agotado".

**Carrito:** la cantidad se elige al añadir. **No hay − / +** (eso es Sprint 2). Si el mueble ya está en el carrito → **409** ("Este mueble ya está en tu carrito. Elimínalo para añadirlo con otra cantidad.").

**Pedido:** estados `PENDIENTE`, `APROBADO`, `RECHAZADO`; número legible `OE-0001`...; datos de entrega = nombre y apellido, teléfono y dirección; un solo número de WhatsApp (`app.whatsapp.numero` en el backend). **Decidido:** los ítems salen del carrito guardado y el carrito se vacía al registrar (un doble clic recibe 409, no duplica). Primero se registra (`POST /api/pedidos`) y después se descarga la Pre-orden (`GET /api/pedidos/{id}/preorden`) las veces que se quiera, sin registrar de nuevo. La dirección de entrega es obligatoria en el pedido.
- **Aprobar/Rechazar:** solo el GESTOR, solo desde `PENDIENTE`. El ADMIN solo consulta.
- **Eliminar Pedido:** el CLIENTE solo sus pedidos y solo en `PENDIENTE` (si no, 403); el GESTOR cualquiera; el **ADMIN no**.
- **Editar Pedido (Sprint 2):** el cliente corrige datos de entrega en `PENDIENTE`; el gestor aplica `ajusteUsd`. No se editan ítems.

## Errores de la API
Siempre `{"mensaje": "..."}`: 400 validación, 401 sin sesión, 403 sin permiso, 404 no existe, 409 conflicto, 500 falla de almacenamiento.

## Documentación por historia (tarjeta de Trello)
Al cerrar cada historia se entrega: **Interfaz Humano-Máquina**, **Arquitectura**, **Estructura de datos** y **Algoritmo**. Backend y frontend documentan cada uno su parte.

## Pendiente (global)
Tasa automática, Carrito (falta eliminar ítem) y, en el frontend, los módulos de Carrito y Pedido. (Pedido está completo en el backend: Registrar, Consultar con Aprobar/Rechazar y Eliminar.) *Actualiza esta sección al cerrar cada historia.*
