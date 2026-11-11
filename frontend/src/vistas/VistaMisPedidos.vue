<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pedidoServicio } from '../servicios/pedidoServicio.js'
import { extraerMensajeError } from '../servicios/muebleServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()

const pedidos = ref([])
const cargando = ref(true)
const eliminandoId = ref(null)
const descargandoPdfId = ref(null)

const mensajeError = ref('')
const mensajeExito = ref('')

// Modal de confirmación para eliminar pedido
const pedidoAEliminar = ref(null)

// Pedido seleccionado para ver detalle expandido
const idPedidoExpandido = ref(null)

onMounted(async () => {
  await cargarPedidos()
})

async function cargarPedidos() {
  cargando.value = true
  mensajeError.value = ''
  try {
    const lista = await pedidoServicio.consultarPedidos()
    pedidos.value = Array.isArray(lista) ? lista : []
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudieron cargar tus pedidos. Por favor intenta nuevamente.'
    )
    pedidos.value = []
  } finally {
    cargando.value = false
  }
}

function alternarDetalle(id) {
  idPedidoExpandido.value = idPedidoExpandido.value === id ? null : id
}

function formatearFecha(isoString) {
  if (!isoString) return '—'
  const fecha = new Date(isoString)
  if (isNaN(fecha.getTime())) return isoString
  return new Intl.DateTimeFormat('es-VE', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(fecha)
}

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

// Descarga de Pre-orden en PDF
async function descargarPreorden(pedido) {
  descargandoPdfId.value = pedido.id
  mensajeError.value = ''
  try {
    await pedidoServicio.descargarPreordenPdf(pedido.id, pedido.numero)
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo descargar la Pre-orden en PDF.'
    )
  } finally {
    descargandoPdfId.value = null
  }
}

function abrirWhatsapp(enlace) {
  if (enlace) {
    window.open(enlace, '_blank', 'noopener,noreferrer')
  }
}

// Confirmación y eliminación de pedido (Solo pedidos en estado PENDIENTE)
function solicitarEliminar(pedido) {
  pedidoAEliminar.value = pedido
}

function cancelarEliminar() {
  pedidoAEliminar.value = null
}

async function confirmarEliminarPedido() {
  if (!pedidoAEliminar.value) return

  const p = pedidoAEliminar.value
  eliminandoId.value = p.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    await pedidoServicio.eliminarPedido(p.id)
    pedidos.value = pedidos.value.filter((item) => item.id !== p.id)
    mensajeExito.value = `El pedido ${p.numero} fue eliminado exitosamente.`
    pedidoAEliminar.value = null
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo eliminar el pedido. Recuerda que solo puedes eliminar pedidos en estado PENDIENTE.'
    )
  } finally {
    eliminandoId.value = null
  }
}
</script>

<template>
  <div class="contenedor vista-mis-pedidos">
    <header class="encabezado-pedidos">
      <div class="informacion-encabezado">
        <h1 class="titulo-pedidos">Mis Pedidos</h1>
        <p class="subtitulo-pedidos">
          Consulta el historial de tus pedidos, descarga tu Pre-orden en PDF o contáctanos por WhatsApp.
        </p>
      </div>
      <button
        type="button"
        class="btn btn-outline btn-recargar"
        :disabled="cargando"
        @click="cargarPedidos"
      >
        ↻ Actualizar
      </button>
    </header>

    <!-- Alertas del Sistema -->
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
      <p>Cargando tus pedidos...</p>
    </div>

    <!-- Estado Vacío -->
    <div v-else-if="pedidos.length === 0" class="tarjeta estado-vacio" aria-label="Sin pedidos">
      <div class="icono-vacio" aria-hidden="true">📦</div>
      <h2>Aún no has registrado ningún pedido</h2>
      <p>Cuando realices un pedido desde el carrito, podrás ver su estado y descargar tu comprobante aquí.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Ir al Catálogo
      </router-link>
    </div>

    <!-- LISTA DE PEDIDOS -->
    <div v-else class="lista-pedidos-contenedor">
      <article
        v-for="pedido in pedidos"
        :key="pedido.id"
        class="tarjeta tarjeta-pedido"
      >
        <!-- Encabezado de la Tarjeta del Pedido -->
        <div class="cabecera-pedido">
          <div class="bloque-identificador">
            <span class="numero-pedido">{{ pedido.numero }}</span>
            <span class="fecha-pedido">{{ formatearFecha(pedido.timestamp) }}</span>
          </div>

          <!-- Insignia de Estado con Colores Reglamentarios: PENDIENTE amarillo, APROBADO verde, RECHAZADO rojo -->
          <div class="bloque-estado">
            <span :class="['insignia-estado', `estado-${pedido.estado?.toLowerCase()}`]">
              {{ pedido.estado }}
            </span>
          </div>
        </div>

        <!-- Resumen de Montos y Productos -->
        <div class="resumen-rapido-pedido">
          <div class="dato-resumen">
            <span class="etiqueta">Total:</span>
            <div class="montos-destacados">
              <span class="monto-usd">{{ formatearUsd(pedido.totalUsd) }}</span>
              <span class="monto-bs">{{ formatearBs(pedido.totalBs) }}</span>
            </div>
          </div>

          <div class="dato-resumen">
            <span class="etiqueta">Productos:</span>
            <span class="valor-texto">
              {{ pedido.items?.length || 0 }} {{ pedido.items?.length === 1 ? 'mueble' : 'muebles' }}
            </span>
          </div>

          <div class="dato-resumen">
            <span class="etiqueta">Entrega a:</span>
            <span class="valor-texto">{{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</span>
          </div>
        </div>

        <!-- Barra de Acciones del Pedido -->
        <div class="acciones-pedido">
          <button
            type="button"
            class="btn btn-outline btn-sm"
            @click="alternarDetalle(pedido.id)"
          >
            {{ idPedidoExpandido === pedido.id ? 'Ocultar Detalle' : 'Ver Detalle' }}
          </button>

          <button
            type="button"
            class="btn btn-secundario btn-sm"
            :disabled="descargandoPdfId === pedido.id"
            @click="descargarPreorden(pedido)"
          >
            📄 {{ descargandoPdfId === pedido.id ? 'Descargando...' : 'Pre-orden (PDF)' }}
          </button>

          <!-- Botón de WhatsApp con el color exclusivo #25D366 -->
          <button
            v-if="pedido.enlaceWhatsapp"
            type="button"
            class="btn-whatsapp-sm"
            title="Enviar mensaje sobre este pedido a WhatsApp"
            @click="abrirWhatsapp(pedido.enlaceWhatsapp)"
          >
            💬 WhatsApp
          </button>

          <!-- Regla Inquebrantable de CLAUDE.md:
               "El CLIENTE solo sus pedidos y solo en PENDIENTE (si no, 403)" -->
          <button
            v-if="pedido.estado === 'PENDIENTE'"
            type="button"
            class="btn btn-peligro-outline btn-sm"
            :disabled="eliminandoId === pedido.id"
            title="Eliminar este pedido pendiente"
            @click="solicitarEliminar(pedido)"
          >
            🗑️ Eliminar
          </button>
        </div>

        <!-- SECCIÓN DE DETALLE EXPANDIBLE -->
        <div v-if="idPedidoExpandido === pedido.id" class="seccion-detalle-pedido">
          <h3 class="titulo-detalle">Detalle del Pedido {{ pedido.numero }}</h3>

          <!-- Datos de entrega -->
          <div class="bloque-datos-entrega">
            <h4>Datos de Entrega</h4>
            <p><strong>Destinatario:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</p>
            <p><strong>Teléfono:</strong> {{ pedido.entrega?.telefono }}</p>
            <p><strong>Dirección:</strong> {{ pedido.entrega?.direccion }}</p>
            <p v-if="pedido.tasaBcv" class="tasa-congelada">
              <strong>Tasa congelada al registrar:</strong> Bs. {{ pedido.tasaBcv }} por 1 USD
            </p>
          </div>

          <!-- Tabla de Ítems del Pedido -->
          <div class="contenedor-tabla-detalle">
            <table class="tabla-detalle-items">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Mueble</th>
                  <th>Cant.</th>
                  <th>Precio Unit.</th>
                  <th>Subtotal</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pedido.items" :key="item.idMueble || item.codigo">
                  <td class="codigo-item">{{ item.codigo }}</td>
                  <td class="nombre-item">{{ item.nombre }}</td>
                  <td class="cantidad-item">{{ item.cantidad }}</td>
                  <td class="precio-item">
                    <div>{{ formatearUsd(item.precioUnitarioUsd) }}</div>
                    <small class="bs-pequeno">{{ formatearBs(item.precioUnitarioBs) }}</small>
                  </td>
                  <td class="subtotal-item">
                    <strong>{{ formatearUsd(item.subtotalUsd) }}</strong>
                    <small class="bs-pequeno">{{ formatearBs(item.subtotalBs) }}</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </article>
    </div>

    <!-- MODAL DE CONFIRMACIÓN PARA ELIMINAR PEDIDO -->
    <div v-if="pedidoAEliminar" class="capa-modal" role="dialog" aria-modal="true" aria-labelledby="titulo-modal-eliminar-pedido">
      <div class="ventana-modal">
        <h3 id="titulo-modal-eliminar-pedido" class="titulo-modal">¿Eliminar pedido {{ pedidoAEliminar.numero }}?</h3>
        <p class="texto-modal">
          ¿Estás seguro de que deseas eliminar este pedido en estado <strong>PENDIENTE</strong>?
          Esta acción no se puede deshacer.
        </p>
        <div class="acciones-modal">
          <button
            type="button"
            class="btn btn-outline"
            :disabled="eliminandoId"
            @click="cancelarEliminar"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="btn btn-peligro"
            :disabled="eliminandoId"
            @click="confirmarEliminarPedido"
          >
            {{ eliminandoId ? 'Eliminando...' : 'Sí, Eliminar Pedido' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vista-mis-pedidos {
  padding-top: 1.5rem;
  padding-bottom: 3.5rem;
}

.encabezado-pedidos {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.titulo-pedidos {
  font-size: 1.85rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.25rem;
}

.subtitulo-pedidos {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
}

.btn-recargar {
  font-size: 0.85rem;
  padding: 0.45rem 0.85rem;
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

/* Lista de Tarjetas de Pedido */
.lista-pedidos-contenedor {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.tarjeta-pedido {
  padding: 1.35rem;
}

.cabecera-pedido {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-borde);
  margin-bottom: 1rem;
}

.bloque-identificador {
  display: flex;
  align-items: baseline;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.numero-pedido {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
}

.fecha-pedido {
  font-size: 0.85rem;
  color: var(--color-texto-secundario);
}

/* Estados Reglamentarios: PENDIENTE amarillo, APROBADO verde, RECHAZADO rojo */
.insignia-estado {
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.3rem 0.75rem;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.estado-pendiente {
  background-color: #FFF8E1;
  color: #F57F17;
  border: 1px solid #FFE082;
}

.estado-aprobado {
  background-color: #E8F5E9;
  color: #2E7D32;
  border: 1px solid #C8E6C9;
}

.estado-rechazado {
  background-color: #FFEBEE;
  color: #C62828;
  border: 1px solid #FFCDD2;
}

/* Resumen Rápido */
.resumen-rapido-pedido {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  margin-bottom: 1.15rem;
}

.dato-resumen {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.dato-resumen .etiqueta {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-texto-secundario);
  text-transform: uppercase;
}

.montos-destacados {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.monto-usd {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
}

.monto-bs {
  font-size: 0.85rem;
  color: var(--color-texto-secundario);
}

.valor-texto {
  font-size: 0.95rem;
  color: var(--color-texto-principal);
  font-weight: 500;
}

/* Barra de Acciones */
.acciones-pedido {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-borde);
}

/* REGLA INQUEBRANTABLE DE CLAUDE.md:
   "Verde #25D366 reservado EXCLUSIVAMENTE para el botón 'Enviar Pedido a WhatsApp'" */
.btn-whatsapp-sm {
  background-color: #25D366;
  color: #FFFFFF;
  border: none;
  border-radius: var(--radio-control);
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: opacity 0.2s ease;
}

.btn-whatsapp-sm:hover {
  opacity: 0.92;
}

.btn-peligro-outline {
  background-color: transparent;
  color: #C62828;
  border: 1px solid #FFCDD2;
}

.btn-peligro-outline:hover:not(:disabled) {
  background-color: #FFEBEE;
  border-color: #C62828;
}

/* Sección de Detalle Expandible */
.seccion-detalle-pedido {
  margin-top: 1.25rem;
  padding: 1.25rem;
  background-color: var(--color-nogal-fondo);
  border-radius: var(--radio-tarjeta);
  border: 1px solid var(--color-borde);
}

.titulo-detalle {
  font-size: 1.05rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.85rem;
}

.bloque-datos-entrega {
  background-color: #FFFFFF;
  padding: 0.95rem;
  border-radius: var(--radio-control);
  margin-bottom: 1rem;
  font-size: 0.9rem;
  line-height: 1.5;
}

.bloque-datos-entrega h4 {
  font-size: 0.95rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.5rem;
}

.tasa-congelada {
  margin-top: 0.4rem;
  color: var(--color-nogal-secundario);
}

.contenedor-tabla-detalle {
  overflow-x: auto;
}

.tabla-detalle-items {
  width: 100%;
  border-collapse: collapse;
  background-color: #FFFFFF;
  border-radius: var(--radio-control);
  overflow: hidden;
  font-size: 0.88rem;
}

.tabla-detalle-items th {
  padding: 0.65rem 0.75rem;
  text-align: left;
  background-color: #F5F5F5;
  border-bottom: 1px solid var(--color-borde);
  font-weight: 600;
  color: var(--color-texto-secundario);
}

.tabla-detalle-items td {
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid var(--color-borde);
  vertical-align: middle;
}

.codigo-item {
  font-weight: 700;
  color: var(--color-nogal-secundario);
}

.nombre-item {
  font-weight: 500;
}

.bs-pequeno {
  display: block;
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
}

/* Modal */
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

@media (max-width: 600px) {
  .cabecera-pedido {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .acciones-pedido {
    flex-direction: column;
    align-items: stretch;
  }

  .acciones-pedido .btn,
  .acciones-pedido .btn-whatsapp-sm {
    width: 100%;
    justify-content: center;
  }
}
</style>
