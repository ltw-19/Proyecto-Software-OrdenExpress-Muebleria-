<script setup>
import { ref, computed, onMounted } from 'vue'
import { sesion } from '../estado/sesion.js'
import { pedidoServicio, extraerMensajeError } from '../servicios/pedidoServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'
import VentanaFlotante from '../componentes/VentanaFlotante.vue'

const pedidos = ref([])
const cargando = ref(false)
const procesandoId = ref(null)
const filtroEstado = ref('TODOS')
const terminoBusqueda = ref('')
const pedidoExpandidoId = ref(null)
const mensajeError = ref('')
const mensajeExito = ref('')

// ── Ventana flotante reutilizable ────────────────────────────────────────────
const ventana = ref({
  mostrar: false,
  tipo: 'error',
  titulo: '',
  mensaje: '',
  textoBoton: 'Entendido',
  accionPendiente: null
})

function cerrarVentana() {
  ventana.value.mostrar = false
  ventana.value.accionPendiente = null
}

async function confirmarVentana() {
  const accion = ventana.value.accionPendiente
  cerrarVentana()
  if (accion) await accion()
}

onMounted(async () => {
  await cargarPedidos()
})

async function cargarPedidos() {
  cargando.value = true
  mensajeError.value = ''
  try {
    const res = await pedidoServicio.consultar()
    pedidos.value = Array.isArray(res) ? res : []
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo cargar la bandeja de pedidos.')
    pedidos.value = []
  } finally {
    cargando.value = false
  }
}

// Filtrado combinado por estado y búsqueda
const pedidosFiltrados = computed(() => {
  return pedidos.value.filter((p) => {
    if (filtroEstado.value !== 'TODOS' && p.estado !== filtroEstado.value) {
      return false
    }

    if (terminoBusqueda.value.trim()) {
      const q = terminoBusqueda.value.toLowerCase().trim()
      const num = (p.numero || '').toLowerCase()
      const cliente = `${p.entrega?.nombre || ''} ${p.entrega?.apellido || ''}`.toLowerCase()
      const tel = (p.entrega?.telefono || '').toLowerCase()
      return num.includes(q) || cliente.includes(q) || tel.includes(q)
    }

    return true
  })
})

function alternarDetalle(id) {
  pedidoExpandidoId.value = pedidoExpandidoId.value === id ? null : id
}

// ACCIONES EXCLUSIVAS DEL GESTOR DE VENTAS
async function aprobarPedido(pedido) {
  if (pedido.estado !== 'PENDIENTE') return

  ventana.value = {
    mostrar: true,
    tipo: 'aviso',
    titulo: 'Aprobar Pedido',
    mensaje: `¿Aprobar el pedido ${pedido.numero}? Se descontará el stock de los muebles en el inventario. Esta acción no se puede deshacer.`,
    textoBoton: 'Sí, Aprobar',
    accionPendiente: () => ejecutarAprobacion(pedido)
  }
}

async function ejecutarAprobacion(pedido) {
  procesandoId.value = pedido.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const actualizado = await pedidoServicio.aprobar(pedido.id)
    pedido.estado = actualizado.estado
    ventana.value = {
      mostrar: true,
      tipo: 'exito',
      titulo: 'Pedido Aprobado',
      mensaje: `El pedido ${pedido.numero} ha sido APROBADO exitosamente. El inventario fue actualizado.`,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
  } catch (error) {
    const errorMsg = extraerMensajeError(error, 'No se pudo aprobar el pedido.')
    ventana.value = {
      mostrar: true,
      tipo: 'error',
      titulo: 'Error al Aprobar',
      mensaje: errorMsg,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
    await cargarPedidos()
  } finally {
    procesandoId.value = null
  }
}

async function rechazarPedido(pedido) {
  if (pedido.estado !== 'PENDIENTE') return

  ventana.value = {
    mostrar: true,
    tipo: 'aviso',
    titulo: 'Rechazar Pedido',
    mensaje: `¿Rechazar el pedido ${pedido.numero}? El pedido quedará en estado RECHAZADO. El stock no se devuelve.`,
    textoBoton: 'Sí, Rechazar',
    accionPendiente: () => ejecutarRechazo(pedido)
  }
}

async function ejecutarRechazo(pedido) {
  procesandoId.value = pedido.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const actualizado = await pedidoServicio.rechazar(pedido.id)
    pedido.estado = actualizado.estado
    ventana.value = {
      mostrar: true,
      tipo: 'info',
      titulo: 'Pedido Rechazado',
      mensaje: `El pedido ${pedido.numero} ha sido RECHAZADO. El stock no fue modificado.`,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
  } catch (error) {
    const errorMsg = extraerMensajeError(error, 'No se pudo rechazar el pedido.')
    ventana.value = {
      mostrar: true,
      tipo: 'error',
      titulo: 'Error al Rechazar',
      mensaje: errorMsg,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
    await cargarPedidos()
  } finally {
    procesandoId.value = null
  }
}

async function eliminarPedido(pedido) {
  ventana.value = {
    mostrar: true,
    tipo: 'error',
    titulo: 'Eliminar Pedido',
    mensaje: `¿Eliminar definitivamente el pedido ${pedido.numero}? Esta acción no devuelve stock y no se puede deshacer.`,
    textoBoton: 'Sí, Eliminar',
    accionPendiente: () => ejecutarEliminacion(pedido)
  }
}

async function ejecutarEliminacion(pedido) {
  procesandoId.value = pedido.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    await pedidoServicio.eliminar(pedido.id)
    ventana.value = {
      mostrar: true,
      tipo: 'exito',
      titulo: 'Pedido Eliminado',
      mensaje: `El pedido ${pedido.numero} ha sido eliminado correctamente.`,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
    await cargarPedidos()
  } catch (error) {
    const errorMsg = extraerMensajeError(error, 'No se pudo eliminar el pedido.')
    ventana.value = {
      mostrar: true,
      tipo: 'error',
      titulo: 'Error al Eliminar',
      mensaje: errorMsg,
      textoBoton: 'Entendido',
      accionPendiente: null
    }
  } finally {
    procesandoId.value = null
  }
}

function formatoMoneda(valor) {
  if (valor == null || isNaN(valor)) return '0,00'
  return new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(valor)
}

function formatearFecha(isoString) {
  if (!isoString) return ''
  try {
    const fecha = new Date(isoString)
    return new Intl.DateTimeFormat('es-VE', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(fecha)
  } catch {
    return isoString
  }
}
</script>

<template>
  <div class="contenedor vista-bandeja">
    <header class="encabezado-bandeja">
      <div>
        <div class="titulo-con-rol">
          <h1>Bandeja de Pedidos Recibidos</h1>
          <span :class="['insignia', `insignia-${sesion.rolActual.value?.toLowerCase()}`]">
            {{ sesion.rolActual.value }}
          </span>
        </div>
        <p v-if="sesion.esGestor.value" class="subtitulo">
          Como Gestor de Ventas, puedes revisar, aprobar (con descuento de stock) o rechazar pre-ordenes pendientes.
        </p>
        <p v-else class="subtitulo">
          Modo Administrador (solo lectura): consulta global de pedidos y estados sin gestión de inventario.
        </p>
      </div>

      <button class="btn btn-outline" :disabled="cargando" @click="cargarPedidos">
        ↻ Actualizar
      </button>
    </header>

    <!-- Ventana Flotante para confirmaciones y resultados (aprobar/rechazar) -->
    <VentanaFlotante
      :mostrar="ventana.mostrar"
      :tipo="ventana.tipo"
      :titulo="ventana.titulo"
      :mensaje="ventana.mensaje"
      :texto-boton="ventana.textoBoton"
      :cerrar-al-fondo="!ventana.accionPendiente"
      :cerrar-con-escape="!ventana.accionPendiente"
      @cerrar="ventana.accionPendiente ? confirmarVentana() : cerrarVentana()"
    >
      <template v-if="ventana.accionPendiente" #acciones>
        <div class="acciones-confirmacion">
          <button type="button" class="btn btn-secundario" @click="cerrarVentana">
            Cancelar
          </button>
          <button
            type="button"
            :class="[
              ventana.titulo.includes('Rechazar') ? 'btn-accion btn-accion-rechazar' :
              ventana.titulo.includes('Eliminar') ? 'btn btn-peligro'               :
              'btn-accion btn-accion-aprobar'
            ]"
            @click="confirmarVentana"
          >
            {{ ventana.textoBoton }}
          </button>
        </div>
      </template>
    </VentanaFlotante>

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

    <!-- Barra de Filtros: Pestañas de Estado y Buscador -->
    <div class="tarjeta barra-controles">
      <div class="pestanas-estado">
        <button
          type="button"
          :class="['btn-pestana', { activo: filtroEstado === 'TODOS' }]"
          @click="filtroEstado = 'TODOS'"
        >
          Todos ({{ pedidos.length }})
        </button>
        <button
          type="button"
          :class="['btn-pestana', { activo: filtroEstado === 'PENDIENTE' }]"
          @click="filtroEstado = 'PENDIENTE'"
        >
          Pendientes ({{ pedidos.filter((p) => p.estado === 'PENDIENTE').length }})
        </button>
        <button
          type="button"
          :class="['btn-pestana', { activo: filtroEstado === 'APROBADO' }]"
          @click="filtroEstado = 'APROBADO'"
        >
          Aprobados ({{ pedidos.filter((p) => p.estado === 'APROBADO').length }})
        </button>
        <button
          type="button"
          :class="['btn-pestana', { activo: filtroEstado === 'RECHAZADO' }]"
          @click="filtroEstado = 'RECHAZADO'"
        >
          Rechazados ({{ pedidos.filter((p) => p.estado === 'RECHAZADO').length }})
        </button>
      </div>

      <div class="buscador-pedidos">
        <input
          v-model="terminoBusqueda"
          type="search"
          class="campo-texto"
          placeholder="Buscar por número OE, cliente o teléfono..."
        />
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-if="cargando" class="cargando">
      Cargando pedidos...
    </div>

    <!-- Estado Vacío -->
    <div v-else-if="pedidosFiltrados.length === 0" class="tarjeta estado-vacio">
      <p>No hay pedidos que coincidan con los filtros aplicados.</p>
    </div>

    <!-- Contenedor Principal (Tabla Desktop + Tarjetas Móvil) -->
    <div v-else class="tarjeta contenedor-tabla-bandeja">
      <!-- Versión Escritorio: Tabla -->
      <table class="tabla-bandeja">
        <thead>
          <tr>
            <th>Número</th>
            <th>Fecha</th>
            <th>Cliente</th>
            <th>Teléfono</th>
            <th>Total USD</th>
            <th>Total Bs</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="pedido in pedidosFiltrados" :key="pedido.id">
            <tr
              class="fila-pedido"
              :class="{ 'fila-expandida': pedidoExpandidoId === pedido.id }"
            >
              <td>
                <div class="celda-numero">
                  <button
                    type="button"
                    class="btn-flecha"
                    :title="pedidoExpandidoId === pedido.id ? 'Contraer vista rápida' : 'Expandir vista rápida'"
                    @click="alternarDetalle(pedido.id)"
                  >
                    {{ pedidoExpandidoId === pedido.id ? '▼' : '►' }}
                  </button>
                  <router-link :to="`/pedidos/${pedido.id}`" class="enlace-numero-pedido">
                    <strong>{{ pedido.numero }}</strong>
                  </router-link>
                </div>
              </td>
              <td>{{ formatearFecha(pedido.timestamp) }}</td>
              <td>
                <strong>{{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</strong>
              </td>
              <td>{{ pedido.entrega?.telefono }}</td>
              <td><strong>$ {{ formatoMoneda(pedido.totalUsd) }}</strong></td>
              <td><small>Bs. {{ formatoMoneda(pedido.totalBs) }}</small></td>
              <td>
                <span
                  :class="[
                    'insignia-estado',
                    `estado-${pedido.estado?.toLowerCase()}`
                  ]"
                >
                  {{ pedido.estado }}
                </span>
              </td>
              <td>
                <div class="grupo-botones-accion">
                  <router-link
                    :to="`/pedidos/${pedido.id}`"
                    class="btn-accion btn-accion-detalle"
                    title="Abrir pantalla completa de detalle del pedido"
                  >
                    👁️ Detalle
                  </router-link>

                  <!-- GESTOR: Puede Aprobar y Rechazar si está PENDIENTE, y Eliminar -->
                  <template v-if="sesion.esGestor.value">
                    <button
                      v-if="pedido.estado === 'PENDIENTE'"
                      type="button"
                      class="btn-accion btn-accion-aprobar"
                      :disabled="procesandoId === pedido.id"
                      title="Aprobar pedido y descontar stock"
                      @click="aprobarPedido(pedido)"
                    >
                      ✓ Aprobar
                    </button>

                    <button
                      v-if="pedido.estado === 'PENDIENTE'"
                      type="button"
                      class="btn-accion btn-accion-rechazar"
                      :disabled="procesandoId === pedido.id"
                      title="Rechazar pedido"
                      @click="rechazarPedido(pedido)"
                    >
                      ✗ Rechazar
                    </button>

                    <button
                      type="button"
                      class="btn-accion btn-accion-eliminar"
                      :disabled="procesandoId === pedido.id"
                      title="Eliminar pedido"
                      @click="eliminarPedido(pedido)"
                    >
                      🗑️
                    </button>
                  </template>
                </div>
              </td>
            </tr>

            <!-- Fila desplegable rápida -->
            <tr v-if="pedidoExpandidoId === pedido.id" class="fila-detalle-expandida">
              <td colspan="8">
                <div class="panel-detalle-pedido">
                  <div class="seccion-detalle-items">
                    <h4>Ítems del Pedido ({{ pedido.items?.length || 0 }} muebles):</h4>
                    <ul class="lista-items-detalle">
                      <li v-for="it in pedido.items" :key="it.idMueble">
                        <span class="codigo-mueble">{{ it.codigo }}</span>
                        <strong class="nombre-mueble">{{ it.nombre }}</strong>
                        <span class="cant-mueble">Cant: {{ it.cantidad }}</span>
                        <span class="monto-unitario">($ {{ formatoMoneda(it.precioUnitarioUsd) }} c/u)</span>
                        <span class="subtotal-mueble">
                          = <strong>$ {{ formatoMoneda(it.subtotalUsd) }}</strong>
                          (Bs. {{ formatoMoneda(it.subtotalBs) }})
                        </span>
                      </li>
                    </ul>
                  </div>

                  <div class="seccion-detalle-entrega">
                    <h4>Datos de Entrega:</h4>
                    <p><strong>Destinatario:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</p>
                    <p><strong>Teléfono:</strong> {{ pedido.entrega?.telefono }}</p>
                    <p><strong>Dirección:</strong> {{ pedido.entrega?.direccion }}</p>
                    <p class="tasa-nota">
                      <strong>Tasa BCV congelada:</strong> 1 USD = Bs. {{ formatoMoneda(pedido.tasaBcv) }}
                    </p>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>

      <!-- Versión Móvil: Lista de Tarjetas Responsive -->
      <div class="lista-tarjetas-movil">
        <article
          v-for="pedido in pedidosFiltrados"
          :key="pedido.id"
          class="tarjeta-pedido-movil"
        >
          <div class="cabecera-movil">
            <div>
              <router-link :to="`/pedidos/${pedido.id}`" class="enlace-numero-pedido">
                <h3>{{ pedido.numero }}</h3>
              </router-link>
              <span class="fecha-movil">{{ formatearFecha(pedido.timestamp) }}</span>
            </div>
            <span
              :class="[
                'insignia-estado',
                `estado-${pedido.estado?.toLowerCase()}`
              ]"
            >
              {{ pedido.estado }}
            </span>
          </div>

          <div class="cuerpo-movil">
            <p><strong>Cliente:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</p>
            <p><strong>Teléfono:</strong> {{ pedido.entrega?.telefono }}</p>
            <div class="totales-movil">
              <strong class="total-usd-movil">$ {{ formatoMoneda(pedido.totalUsd) }} USD</strong>
              <span class="total-bs-movil">Bs. {{ formatoMoneda(pedido.totalBs) }}</span>
            </div>
          </div>

          <div class="pie-movil">
            <router-link
              :to="`/pedidos/${pedido.id}`"
              class="btn btn-outline btn-xs"
            >
              👁️ Ver Detalle
            </router-link>

            <div v-if="sesion.esGestor.value" class="acciones-movil-gestor">
              <button
                v-if="pedido.estado === 'PENDIENTE'"
                type="button"
                class="btn-accion btn-accion-aprobar"
                :disabled="procesandoId === pedido.id"
                @click="aprobarPedido(pedido)"
              >
                ✓ Aprobar
              </button>
              <button
                v-if="pedido.estado === 'PENDIENTE'"
                type="button"
                class="btn-accion btn-accion-rechazar"
                :disabled="procesandoId === pedido.id"
                @click="rechazarPedido(pedido)"
              >
                ✗ Rechazar
              </button>
              <button
                type="button"
                class="btn-accion btn-accion-eliminar"
                :disabled="procesandoId === pedido.id"
                @click="eliminarPedido(pedido)"
              >
                🗑️
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vista-bandeja {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.encabezado-bandeja {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.titulo-con-rol {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.cargando, .estado-vacio {
  text-align: center;
  padding: 3rem;
  color: var(--color-texto-secundario);
}

.barra-controles {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.pestanas-estado {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-pestana {
  background: transparent;
  border: 1px solid var(--color-borde);
  padding: 0.45rem 0.85rem;
  border-radius: var(--radio-control);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  color: var(--color-texto-principal);
  transition: all 0.2s ease;
}

.btn-pestana:hover {
  background-color: var(--color-nogal-fondo);
}

.btn-pestana.activo {
  background-color: var(--color-nogal-primario);
  color: #FFFFFF;
  border-color: var(--color-nogal-primario);
}

.buscador-pedidos {
  min-width: 280px;
  flex: 1;
  max-width: 360px;
}

.contenedor-tabla-bandeja {
  padding: 0;
  overflow: hidden;
}

.tabla-bandeja {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.tabla-bandeja th {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  padding: 0.85rem 1rem;
  text-align: left;
  font-weight: 600;
  white-space: nowrap;
}

.tabla-bandeja td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-borde);
  vertical-align: middle;
}

.celda-numero {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-flecha {
  background: transparent;
  border: none;
  color: var(--color-nogal-secundario);
  cursor: pointer;
  font-size: 0.8rem;
  padding: 0.2rem;
}

.enlace-numero-pedido {
  color: var(--color-nogal-primario);
  text-decoration: none;
}

.enlace-numero-pedido:hover {
  text-decoration: underline;
}

.insignia-estado {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.estado-pendiente {
  background-color: var(--color-estado-pendiente-fondo);
  color: var(--color-estado-pendiente);
  border: 1px solid #FFE082;
}

.estado-aprobado {
  background-color: var(--color-estado-aprobado-fondo);
  color: var(--color-estado-aprobado);
  border: 1px solid #A5D6A7;
}

.estado-rechazado {
  background-color: var(--color-estado-rechazado-fondo);
  color: var(--color-estado-rechazado);
  border: 1px solid #EF9A9A;
}

.grupo-botones-accion {
  display: flex;
  gap: 0.4rem;
  align-items: center;
  flex-wrap: wrap;
}

.btn-accion {
  border: none;
  border-radius: 4px;
  padding: 0.35rem 0.6rem;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.btn-accion-detalle {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  border: 1px solid var(--color-nogal-claro);
}

.btn-accion-detalle:hover {
  background-color: var(--color-nogal-claro);
}

.btn-accion-aprobar {
  background-color: var(--color-exito-fondo);
  color: var(--color-exito);
  border: 1px solid #C8E6C9;
}

.btn-accion-aprobar:hover:not(:disabled) {
  background-color: #C8E6C9;
}

.btn-accion-rechazar {
  background-color: var(--color-error-fondo);
  color: var(--color-error);
  border: 1px solid #FFCDD2;
}

.btn-accion-rechazar:hover:not(:disabled) {
  background-color: #FFCDD2;
}

.btn-accion-eliminar {
  background-color: transparent;
  color: var(--color-texto-mutado);
  font-size: 0.95rem;
}

.btn-accion-eliminar:hover:not(:disabled) {
  color: var(--color-error);
}

.btn-xs {
  padding: 0.3rem 0.65rem;
  font-size: 0.8rem;
}

/* Fila de detalle expandida */
.fila-detalle-expandida td {
  background-color: #FAFAFA;
  padding: 1.25rem 1.5rem;
}

.panel-detalle-pedido {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

.lista-items-detalle {
  list-style: none;
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.lista-items-detalle li {
  display: flex;
  gap: 0.5rem;
  align-items: baseline;
  flex-wrap: wrap;
  font-size: 0.85rem;
}

.codigo-mueble {
  font-weight: 700;
  color: var(--color-nogal-secundario);
}

.seccion-detalle-entrega h4,
.seccion-detalle-items h4 {
  font-size: 0.95rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.4rem;
}

.seccion-detalle-entrega p {
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}

.tasa-nota {
  margin-top: 0.5rem;
  color: var(--color-texto-mutado);
}

/* Versión móvil en tarjetas */
.lista-tarjetas-movil {
  display: none;
  padding: 1rem;
  flex-direction: column;
  gap: 1rem;
}

.tarjeta-pedido-movil {
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  padding: 1rem;
  background-color: var(--color-superficie);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cabecera-movil {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid var(--color-borde);
  padding-bottom: 0.6rem;
}

.cabecera-movil h3 {
  margin-bottom: 0.1rem;
  font-size: 1.1rem;
}

.fecha-movil {
  font-size: 0.78rem;
  color: var(--color-texto-mutado);
}

.cuerpo-movil p {
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
}

.totales-movil {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  background-color: var(--color-nogal-fondo);
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  margin-top: 0.4rem;
}

.total-usd-movil {
  color: var(--color-nogal-primario);
  font-size: 1rem;
}

.total-bs-movil {
  font-size: 0.85rem;
  color: var(--color-texto-principal);
}

.pie-movil {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-borde);
}

.acciones-movil-gestor {
  display: flex;
  gap: 0.4rem;
}

@media (max-width: 860px) {
  .barra-controles {
    flex-direction: column;
    align-items: stretch;
  }

  .buscador-pedidos {
    max-width: 100%;
  }

  .tabla-bandeja {
    display: none;
  }

  .lista-tarjetas-movil {
    display: flex;
  }
}

/* Botonera de confirmación dentro de la ventana flotante */
.acciones-confirmacion {
  display: flex;
  flex-wrap: nowrap;
  gap: 0.75rem;
  width: 100%;
}

.acciones-confirmacion .btn-accion,
.acciones-confirmacion .btn {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.65rem 1rem;
  font-weight: 600;
  border-radius: var(--radio-control);
  cursor: pointer;
  border: none;
  transition: background-color 0.2s ease;
}
</style>