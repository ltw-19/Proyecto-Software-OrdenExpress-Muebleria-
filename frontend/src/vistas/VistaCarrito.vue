<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sesion } from '../estado/sesion.js'
import { carritoEstado } from '../estado/carritoEstado.js'
import { carritoServicio } from '../servicios/carritoServicio.js'
import { pedidoServicio } from '../servicios/pedidoServicio.js'
import { extraerMensajeError } from '../servicios/muebleServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()

const cargando = ref(true)
const procesandoEliminacion = ref(false)
const registrandoPedido = ref(false)
const descargandoPdf = ref(false)

const mensajeError = ref('')
const mensajeExito = ref('')

// Pedido registrado recientemente para descarga de PDF y WhatsApp
const pedidoCreado = ref(null)

// Modal de confirmación para eliminar ítem
const itemAEliminar = ref(null)

// Datos de entrega del pedido precargados desde la sesión
const formularioEntrega = reactive({
  nombre: '',
  apellido: '',
  telefono: '',
  direccion: ''
})

const erroresValidacion = reactive({
  nombre: '',
  apellido: '',
  telefono: '',
  direccion: ''
})

onMounted(async () => {
  precargarDatosUsuario()
  await cargarCarrito()
})

function precargarDatosUsuario() {
  const u = sesion.usuario.value
  if (u) {
    formularioEntrega.nombre = u.nombre || ''
    formularioEntrega.apellido = u.apellido || ''
    formularioEntrega.telefono = u.telefono || ''
    formularioEntrega.direccion = u.direccion || ''
  }
}

async function cargarCarrito() {
  cargando.value = true
  mensajeError.value = ''
  try {
    const respuesta = await carritoServicio.consultarCarrito()
    carritoEstado.sincronizarConRespuesta(respuesta)
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo cargar el carrito de compras. Intenta nuevamente.'
    )
  } finally {
    cargando.value = false
  }
}

// Formateo bimonetario estándar es-VE (el frontend nunca calcula los montos)
function formatearUsd(monto) {
  if (monto == null || isNaN(monto)) return '$ 0,00'
  return '$ ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(monto)
}

function formatearBs(monto) {
  if (monto == null || isNaN(monto)) return 'Bs. —'
  return 'Bs. ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(monto)
}

// Confirmación y eliminación de ítem
function solicitarEliminarItem(item) {
  itemAEliminar.value = item
}

function cancelarEliminar() {
  itemAEliminar.value = null
}

async function confirmarEliminarItem() {
  if (!itemAEliminar.value) return

  procesandoEliminacion.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const nombre = itemAEliminar.value.nombre
    const carritoActualizado = await carritoServicio.eliminarItem(itemAEliminar.value.idMueble)
    carritoEstado.sincronizarConRespuesta(carritoActualizado)
    mensajeExito.value = `Se eliminó "${nombre}" del carrito.`
    itemAEliminar.value = null
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo eliminar el ítem del carrito.'
    )
  } finally {
    procesandoEliminacion.value = false
  }
}

// Validación estricta 100% de los datos de entrega antes de enviar
function validarFormulario() {
  let valido = true
  erroresValidacion.nombre = ''
  erroresValidacion.apellido = ''
  erroresValidacion.telefono = ''
  erroresValidacion.direccion = ''

  const nom = formularioEntrega.nombre?.trim() || ''
  if (!nom) {
    erroresValidacion.nombre = 'El nombre es obligatorio.'
    valido = false
  } else if (nom.length > 100) {
    erroresValidacion.nombre = 'El nombre admite máximo 100 caracteres.'
    valido = false
  }

  const ape = formularioEntrega.apellido?.trim() || ''
  if (!ape) {
    erroresValidacion.apellido = 'El apellido es obligatorio.'
    valido = false
  } else if (ape.length > 100) {
    erroresValidacion.apellido = 'El apellido admite máximo 100 caracteres.'
    valido = false
  }

  // Quitar espacios, guiones y paréntesis para validar formato internacional/nacional
  const telLimpio = (formularioEntrega.telefono || '').replace(/[\s\-\(\)]/g, '')
  const telRegex = /^\+?[0-9]{10,15}$/
  if (!telLimpio) {
    erroresValidacion.telefono = 'El teléfono es obligatorio.'
    valido = false
  } else if (!telRegex.test(telLimpio)) {
    erroresValidacion.telefono = 'El teléfono debe tener entre 10 y 15 dígitos numéricos (puede iniciar con +).'
    valido = false
  }

  const dir = formularioEntrega.direccion?.trim() || ''
  if (!dir) {
    erroresValidacion.direccion = 'La dirección de entrega es obligatoria para procesar el pedido.'
    valido = false
  } else if (dir.length > 200) {
    erroresValidacion.direccion = 'La dirección admite máximo 200 caracteres.'
    valido = false
  }

  return valido
}

// Registro del pedido
async function procesarRegistroPedido() {
  if (carritoEstado.items.value.length === 0) {
    mensajeError.value = 'Tu carrito está vacío. Añade al menos un mueble antes de registrar el pedido.'
    return
  }

  if (!validarFormulario()) {
    return
  }

  registrandoPedido.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const telLimpio = formularioEntrega.telefono.replace(/[\s\-\(\)]/g, '')
    const datosEnvio = {
      nombre: formularioEntrega.nombre.trim(),
      apellido: formularioEntrega.apellido.trim(),
      telefono: telLimpio,
      direccion: formularioEntrega.direccion.trim()
    }

    const respuestaPedido = await pedidoServicio.registrarPedido(datosEnvio)
    pedidoCreado.value = respuestaPedido
    carritoEstado.vaciar()
    mensajeExito.value = `¡Pedido ${respuestaPedido.numero} registrado con éxito!`
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo registrar el pedido. Por favor verifica los datos e intenta nuevamente.'
    )
  } finally {
    registrandoPedido.value = false
  }
}

// Descarga del documento Pre-orden en PDF
async function descargarPreorden() {
  if (!pedidoCreado.value?.id) return

  descargandoPdf.value = true
  try {
    await pedidoServicio.descargarPreordenPdf(pedidoCreado.value.id, pedidoCreado.value.numero)
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo descargar la Pre-orden en PDF. Puedes intentar descargarla desde Mis Pedidos.'
    )
  } finally {
    descargandoPdf.value = false
  }
}

function abrirWhatsapp() {
  if (pedidoCreado.value?.enlaceWhatsapp) {
    window.open(pedidoCreado.value.enlaceWhatsapp, '_blank', 'noopener,noreferrer')
  }
}
</script>

<template>
  <div class="contenedor vista-carrito">
    <header class="encabezado-carrito">
      <h1 class="titulo-carrito">Mi Carrito de Compras</h1>
      <p class="subtitulo-carrito">
        Revisa los muebles seleccionados y confirma los datos de entrega de tu pedido.
      </p>
    </header>

    <!-- Alertas informativas y de error -->
    <AlertaMensaje
      v-if="mensajeError"
      tipo="error"
      :mensaje="mensajeError"
      @cerrar="mensajeError = ''"
    />

    <AlertaMensaje
      v-if="mensajeExito"
      tipo="exito"
      :mensaje="mensajeExito"
      @cerrar="mensajeExito = ''"
    />

    <!-- Estado de Carga -->
    <div v-if="cargando" class="estado-carga">
      <div class="spinner" aria-hidden="true"></div>
      <p>Cargando tu carrito...</p>
    </div>

    <!-- PANTALLA DE ÉXITO TRAS REGISTRAR EL PEDIDO -->
    <section v-else-if="pedidoCreado" class="tarjeta seccion-exito-pedido" aria-label="Confirmación de pedido">
      <div class="icono-exito" aria-hidden="true">✓</div>
      <h2 class="titulo-exito">¡Pedido Registrado con Éxito!</h2>
      <p class="numero-pedido-destacado">
        Número de Pedido: <strong>{{ pedidoCreado.numero }}</strong>
      </p>
      <p class="explicacion-flujo">
        Tu pedido ha sido registrado en estado <strong>PENDIENTE</strong>. Para completar la compra,
        descarga tu <strong>Pre-orden en PDF</strong> y envíala a través de <strong>WhatsApp</strong> al Gestor de Ventas para coordinar el pago y la entrega.
      </p>

      <div class="resumen-pedido-creado">
        <div class="dato-resumen">
          <span class="etiqueta">Total en USD:</span>
          <span class="valor-usd">{{ formatearUsd(pedidoCreado.totalUsd) }}</span>
        </div>
        <div class="dato-resumen">
          <span class="etiqueta">Total en Bolívares:</span>
          <span class="valor-bs">{{ formatearBs(pedidoCreado.totalBs) }}</span>
        </div>
        <div class="dato-resumen">
          <span class="etiqueta">Entrega a:</span>
          <span class="valor-texto">{{ pedidoCreado.entrega?.nombre }} {{ pedidoCreado.entrega?.apellido }}</span>
        </div>
        <div class="dato-resumen">
          <span class="etiqueta">Dirección:</span>
          <span class="valor-texto">{{ pedidoCreado.entrega?.direccion }}</span>
        </div>
      </div>

      <!-- Acciones de cierre de flujo: PDF y WhatsApp exclusivo #25D366 -->
      <div class="acciones-pedido-exito">
        <button
          type="button"
          class="btn btn-secundario btn-accion-pdf"
          :disabled="descargandoPdf"
          @click="descargarPreorden"
        >
          📄 {{ descargandoPdf ? 'Descargando...' : 'Descargar Pre-orden (PDF)' }}
        </button>

        <button
          v-if="pedidoCreado.enlaceWhatsapp"
          type="button"
          class="btn-whatsapp"
          @click="abrirWhatsapp"
        >
          <span class="icono-wa" aria-hidden="true">💬</span>
          <span>Enviar Pedido a WhatsApp</span>
        </button>

        <router-link to="/mis-pedidos" class="btn btn-outline btn-ver-pedidos">
          📋 Ver Mis Pedidos
        </router-link>

        <router-link to="/catalogo" class="enlace-volver-catalogo">
          ← Seguir explorando el catálogo
        </router-link>
      </div>
    </section>

    <!-- ESTADO CARRITO VACÍO -->
    <section v-else-if="carritoEstado.items.value.length === 0" class="tarjeta estado-vacio" aria-label="Carrito vacío">
      <div class="icono-vacio" aria-hidden="true">🛒</div>
      <h2>Tu carrito está vacío</h2>
      <p>No tienes productos agregados en este momento. Explora nuestro catálogo y añade las piezas que te gusten.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Explorar Catálogo
      </router-link>
    </section>

    <!-- CONTENIDO DEL CARRITO CON ÍTEMS -->
    <div v-else class="cuadricula-contenido-carrito">
      <!-- Columna Izquierda: Tabla y Lista de Ítems -->
      <section class="tarjeta seccion-items" aria-label="Productos en el carrito">
        <h2 class="subtitulo-seccion">Productos en tu Carrito ({{ carritoEstado.conteoUnidades.value }} unidades)</h2>

        <!-- Tabla para Escritorio -->
        <div class="contenedor-tabla-escritorio">
          <table class="tabla-carrito">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Código</th>
                <th>Precio Unit.</th>
                <th>Cantidad</th>
                <th>Subtotal</th>
                <th class="col-accion">Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in carritoEstado.items.value" :key="item.idMueble">
                <td class="celda-producto">
                  <div class="mini-info-producto">
                    <img
                      v-if="item.urlFoto"
                      :src="item.urlFoto"
                      :alt="item.nombre"
                      class="foto-miniatura"
                      loading="lazy"
                    />
                    <div class="detalles-producto-texto">
                      <span class="nombre-producto-tabla">{{ item.nombre }}</span>
                      <span v-if="!item.disponible" class="insignia-agotado-tabla">Agotado</span>
                    </div>
                  </div>
                </td>
                <td class="celda-codigo">{{ item.codigo || '—' }}</td>
                <td class="celda-precios">
                  <div class="monto-usd-tabla">{{ formatearUsd(item.precioUsd) }}</div>
                  <div class="monto-bs-tabla">{{ formatearBs(item.precioBs) }}</div>
                </td>
                <td class="celda-cantidad">
                  <!-- Regla inquebrantable de CLAUDE.md: Cantidades de solo lectura, sin botones -/+ -->
                  <span class="badge-cantidad-fija">{{ item.cantidad }}</span>
                </td>
                <td class="celda-subtotal">
                  <div class="monto-usd-tabla">{{ formatearUsd(item.subtotalUsd) }}</div>
                  <div class="monto-bs-tabla">{{ formatearBs(item.subtotalBs) }}</div>
                </td>
                <td class="celda-accion">
                  <button
                    type="button"
                    class="btn-eliminar-item"
                    title="Eliminar mueble del carrito"
                    aria-label="Eliminar mueble del carrito"
                    @click="solicitarEliminarItem(item)"
                  >
                    🗑️
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Tarjetas para Móviles (Mobile-First) -->
        <div class="lista-items-movil">
          <article
            v-for="item in carritoEstado.items.value"
            :key="`movil-${item.idMueble}`"
            class="tarjeta-item-movil"
          >
            <div class="cabecera-item-movil">
              <span class="codigo-movil">{{ item.codigo }}</span>
              <button
                type="button"
                class="btn-eliminar-item"
                title="Eliminar mueble del carrito"
                aria-label="Eliminar mueble del carrito"
                @click="solicitarEliminarItem(item)"
              >
                🗑️
              </button>
            </div>
            <div class="cuerpo-item-movil">
              <img
                v-if="item.urlFoto"
                :src="item.urlFoto"
                :alt="item.nombre"
                class="foto-miniatura-movil"
                loading="lazy"
              />
              <div class="info-movil">
                <h3 class="nombre-mueble-movil">{{ item.nombre }}</h3>
                <span v-if="!item.disponible" class="insignia-agotado-tabla">Agotado</span>
                <p class="cantidad-movil">
                  Cantidad: <strong>{{ item.cantidad }}</strong>
                </p>
                <div class="precios-movil">
                  <span class="subtotal-etiqueta">Subtotal:</span>
                  <span class="monto-usd-movil">{{ formatearUsd(item.subtotalUsd) }}</span>
                  <span class="monto-bs-movil">{{ formatearBs(item.subtotalBs) }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <p class="nota-cantidades-fijas">
          ℹ️ Para modificar la cantidad de un mueble, elimínalo del carrito y vuelve a añadirlo desde el catálogo con la cantidad deseada.
        </p>
      </section>

      <!-- Columna Derecha: Totales y Formulario de Entrega -->
      <aside class="columna-resumen-compra">
        <!-- Tarjeta de Totales -->
        <div class="tarjeta seccion-totales">
          <h2 class="subtitulo-seccion">Resumen del Pedido</h2>
          <div class="fila-total">
            <span class="etiqueta-total">Total en USD:</span>
            <span class="monto-total-usd">{{ formatearUsd(carritoEstado.totalUsd.value) }}</span>
          </div>
          <div class="fila-total fila-bs">
            <span class="etiqueta-total">Total en Bolívares:</span>
            <span class="monto-total-bs">{{ formatearBs(carritoEstado.totalBs.value) }}</span>
          </div>
          <p class="aclaratoria-precios">
            * Los precios en Bolívares y totales son calculados oficialmente por el servidor según la tasa vigente y se congelarán al registrar el pedido.
          </p>
        </div>

        <!-- Tarjeta de Datos de Entrega y Registro de Pedido -->
        <div class="tarjeta seccion-formulario-entrega">
          <h2 class="subtitulo-seccion">Datos de Entrega</h2>
          <form @submit.prevent="procesarRegistroPedido" novalidate>
            <div class="campo-grupo">
              <label for="entrega-nombre" class="etiqueta-campo">Nombre *</label>
              <input
                id="entrega-nombre"
                v-model="formularioEntrega.nombre"
                type="text"
                maxlength="100"
                class="campo-texto"
                :class="{ 'campo-error': erroresValidacion.nombre }"
                placeholder="Nombre del destinatario"
                required
              />
              <span v-if="erroresValidacion.nombre" class="texto-error">{{ erroresValidacion.nombre }}</span>
            </div>

            <div class="campo-grupo">
              <label for="entrega-apellido" class="etiqueta-campo">Apellido *</label>
              <input
                id="entrega-apellido"
                v-model="formularioEntrega.apellido"
                type="text"
                maxlength="100"
                class="campo-texto"
                :class="{ 'campo-error': erroresValidacion.apellido }"
                placeholder="Apellido del destinatario"
                required
              />
              <span v-if="erroresValidacion.apellido" class="texto-error">{{ erroresValidacion.apellido }}</span>
            </div>

            <div class="campo-grupo">
              <label for="entrega-telefono" class="etiqueta-campo">Teléfono de Contacto *</label>
              <input
                id="entrega-telefono"
                v-model="formularioEntrega.telefono"
                type="tel"
                class="campo-texto"
                :class="{ 'campo-error': erroresValidacion.telefono }"
                placeholder="Ej: 04121234567 o +584121234567"
                required
              />
              <span v-if="erroresValidacion.telefono" class="texto-error">{{ erroresValidacion.telefono }}</span>
            </div>

            <div class="campo-grupo">
              <label for="entrega-direccion" class="etiqueta-campo">Dirección de Entrega *</label>
              <textarea
                id="entrega-direccion"
                v-model="formularioEntrega.direccion"
                rows="3"
                maxlength="200"
                class="campo-texto area-texto"
                :class="{ 'campo-error': erroresValidacion.direccion }"
                placeholder="Dirección completa de entrega (máx. 200 caracteres)"
                required
              ></textarea>
              <div class="contador-caracteres">
                <span>{{ (formularioEntrega.direccion || '').length }}/200 caracteres</span>
                <span v-if="erroresValidacion.direccion" class="texto-error">{{ erroresValidacion.direccion }}</span>
              </div>
            </div>

            <button
              type="submit"
              class="btn btn-primario btn-confirmar-pedido"
              :disabled="registrandoPedido"
            >
              {{ registrandoPedido ? 'Registrando Pedido...' : 'Registrar Pedido' }}
            </button>
          </form>
        </div>
      </aside>
    </div>

    <!-- MODAL DE CONFIRMACIÓN PARA ELIMINAR ÍTEM -->
    <div v-if="itemAEliminar" class="capa-modal" role="dialog" aria-modal="true" aria-labelledby="titulo-modal-eliminar">
      <div class="ventana-modal">
        <h3 id="titulo-modal-eliminar" class="titulo-modal">¿Eliminar mueble del carrito?</h3>
        <p class="texto-modal">
          ¿Estás seguro de que deseas eliminar <strong>"{{ itemAEliminar.nombre }}"</strong> ({{ itemAEliminar.codigo }}) de tu carrito?
        </p>
        <div class="acciones-modal">
          <button
            type="button"
            class="btn btn-outline"
            :disabled="procesandoEliminacion"
            @click="cancelarEliminar"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="btn btn-peligro"
            :disabled="procesandoEliminacion"
            @click="confirmarEliminarItem"
          >
            {{ procesandoEliminacion ? 'Eliminando...' : 'Sí, Eliminar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vista-carrito {
  padding-top: 1.5rem;
  padding-bottom: 3.5rem;
}

.encabezado-carrito {
  margin-bottom: 1.5rem;
}

.titulo-carrito {
  font-size: 1.85rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.25rem;
}

.subtitulo-carrito {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
}

.estado-carga {
  text-align: center;
  padding: 4rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-borde);
  border-top-color: var(--color-nogal-primario);
  border-radius: 50%;
  animation: girar 0.8s linear infinite;
}

@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}

.estado-vacio {
  text-align: center;
  padding: 3.5rem 1.5rem;
  max-width: 520px;
  margin: 2rem auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.icono-vacio {
  font-size: 3.5rem;
  line-height: 1;
}

/* Pantalla de Confirmación de Pedido Exitoso */
.seccion-exito-pedido {
  text-align: center;
  max-width: 680px;
  margin: 2rem auto;
  padding: 2.5rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
}

.icono-exito {
  width: 65px;
  height: 65px;
  border-radius: 50%;
  background-color: #E8F5E9;
  color: #2E7D32;
  font-size: 2.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.titulo-exito {
  font-size: 1.75rem;
  color: var(--color-nogal-primario);
}

.numero-pedido-destacado {
  font-size: 1.15rem;
  color: var(--color-texto-principal);
}

.numero-pedido-destacado strong {
  color: var(--color-nogal-secundario);
  font-size: 1.25rem;
}

.explicacion-flujo {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
  line-height: 1.5;
  max-width: 560px;
}

.resumen-pedido-creado {
  background-color: var(--color-nogal-fondo);
  border-radius: var(--radio-tarjeta);
  padding: 1.25rem;
  width: 100%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  text-align: left;
}

.dato-resumen {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  font-size: 0.9rem;
}

.dato-resumen .etiqueta {
  font-weight: 600;
  color: var(--color-texto-principal);
}

.valor-usd {
  font-weight: 700;
  color: var(--color-nogal-primario);
}

.valor-bs {
  color: var(--color-texto-secundario);
}

.acciones-pedido-exito {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  width: 100%;
  max-width: 400px;
  margin-top: 0.5rem;
}

.btn-accion-pdf {
  font-weight: 600;
  padding: 0.8rem 1.25rem;
  font-size: 0.95rem;
}

/* REGLA INQUEBRANTABLE DE CLAUDE.md:
   "Verde #25D366 reservado EXCLUSIVAMENTE para el botón 'Enviar Pedido a WhatsApp'" */
.btn-whatsapp {
  background-color: #25D366;
  color: #FFFFFF;
  border: none;
  border-radius: var(--radio-control);
  padding: 0.85rem 1.25rem;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  transition: opacity 0.2s ease, transform 0.1s ease;
  box-shadow: 0 3px 6px rgba(37, 211, 102, 0.35);
}

.btn-whatsapp:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.icono-wa {
  font-size: 1.2rem;
}

.btn-ver-pedidos {
  padding: 0.75rem 1rem;
}

.enlace-volver-catalogo {
  color: var(--color-nogal-primario);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.enlace-volver-catalogo:hover {
  text-decoration: underline;
}

/* Cuadrícula de Contenido */
.cuadricula-contenido-carrito {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 1.5rem;
  align-items: start;
}

.subtitulo-seccion {
  font-size: 1.15rem;
  color: var(--color-nogal-primario);
  margin-bottom: 1rem;
}

/* Tabla de Ítems (Escritorio) */
.contenedor-tabla-escritorio {
  overflow-x: auto;
}

.tabla-carrito {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.tabla-carrito th {
  padding: 0.75rem 0.65rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-texto-secundario);
  border-bottom: 2px solid var(--color-borde);
}

.tabla-carrito td {
  padding: 0.85rem 0.65rem;
  border-bottom: 1px solid var(--color-borde);
  vertical-align: middle;
}

.mini-info-producto {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.foto-miniatura {
  width: 55px;
  height: 55px;
  object-fit: cover;
  border-radius: 6px;
  background-color: var(--color-borde);
}

.detalles-producto-texto {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nombre-producto-tabla {
  font-weight: 600;
  color: var(--color-texto-principal);
  font-size: 0.95rem;
}

.insignia-agotado-tabla {
  display: inline-block;
  background-color: #FFEBEE;
  color: #C62828;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.celda-codigo {
  font-size: 0.85rem;
  color: var(--color-texto-secundario);
  font-weight: 600;
}

.monto-usd-tabla {
  font-weight: 700;
  color: var(--color-nogal-primario);
  font-size: 0.95rem;
}

.monto-bs-tabla {
  font-size: 0.8rem;
  color: var(--color-texto-mutado);
}

.badge-cantidad-fija {
  display: inline-block;
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  font-weight: 700;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.95rem;
}

.btn-eliminar-item {
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 1.1rem;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-eliminar-item:hover {
  background-color: #FFEBEE;
  border-color: #FFCDD2;
}

.nota-cantidades-fijas {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: var(--color-texto-secundario);
  line-height: 1.4;
}

/* Lista de Ítems para Móviles */
.lista-items-movil {
  display: none;
}

/* Columna Derecha */
.columna-resumen-compra {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.seccion-totales {
  padding: 1.25rem;
}

.fila-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.5rem;
}

.etiqueta-total {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-texto-principal);
}

.monto-total-usd {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
}

.fila-bs {
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-borde);
}

.monto-total-bs {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-texto-secundario);
}

.aclaratoria-precios {
  margin-top: 0.85rem;
  font-size: 0.78rem;
  color: var(--color-texto-mutado);
  line-height: 1.35;
}

/* Formulario de Entrega */
.seccion-formulario-entrega {
  padding: 1.25rem;
}

.campo-grupo {
  margin-bottom: 0.95rem;
}

.etiqueta-campo {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.3rem;
  color: var(--color-texto-principal);
}

.area-texto {
  resize: vertical;
}

.campo-error {
  border-color: #C62828 !important;
  background-color: #FFF8F8;
}

.texto-error {
  display: block;
  font-size: 0.78rem;
  color: #C62828;
  margin-top: 0.25rem;
}

.contador-caracteres {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
  margin-top: 0.2rem;
}

.btn-confirmar-pedido {
  width: 100%;
  padding: 0.85rem;
  font-size: 1rem;
  font-weight: 700;
  margin-top: 0.5rem;
}

/* Modal de Confirmación */
.capa-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.ventana-modal {
  background-color: var(--color-superficie);
  border-radius: var(--radio-tarjeta);
  padding: 1.5rem;
  max-width: 440px;
  width: 100%;
  box-shadow: var(--sombra-elevada);
}

.titulo-modal {
  font-size: 1.2rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.65rem;
}

.texto-modal {
  font-size: 0.95rem;
  color: var(--color-texto-principal);
  line-height: 1.45;
  margin-bottom: 1.25rem;
}

.acciones-modal {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-peligro {
  background-color: #C62828;
  color: #FFFFFF;
  border: none;
  border-radius: var(--radio-control);
  padding: 0.65rem 1rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-peligro:hover:not(:disabled) {
  background-color: #B71C1C;
}

/* Adaptación Responsive */
@media (max-width: 860px) {
  .cuadricula-contenido-carrito {
    grid-template-columns: 1fr;
  }

  .contenedor-tabla-escritorio {
    display: none;
  }

  .lista-items-movil {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
  }

  .tarjeta-item-movil {
    border: 1px solid var(--color-borde);
    border-radius: var(--radio-control);
    padding: 0.85rem;
    background-color: #FFFFFF;
  }

  .cabecera-item-movil {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
  }

  .codigo-movil {
    font-size: 0.8rem;
    font-weight: 700;
    color: var(--color-nogal-secundario);
    background-color: var(--color-nogal-fondo);
    padding: 0.15rem 0.45rem;
    border-radius: 4px;
  }

  .cuerpo-item-movil {
    display: flex;
    gap: 0.85rem;
  }

  .foto-miniatura-movil {
    width: 70px;
    height: 70px;
    object-fit: cover;
    border-radius: 6px;
    background-color: var(--color-borde);
  }

  .info-movil {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .nombre-mueble-movil {
    font-size: 0.95rem;
    color: var(--color-texto-principal);
    margin: 0;
  }

  .cantidad-movil {
    font-size: 0.85rem;
    color: var(--color-texto-secundario);
    margin: 0;
  }

  .precios-movil {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 0.4rem;
    margin-top: 0.25rem;
  }

  .subtotal-etiqueta {
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--color-texto-secundario);
  }

  .monto-usd-movil {
    font-weight: 700;
    color: var(--color-nogal-primario);
    font-size: 0.95rem;
  }

  .monto-bs-movil {
    font-size: 0.8rem;
    color: var(--color-texto-mutado);
  }
}
</style>
