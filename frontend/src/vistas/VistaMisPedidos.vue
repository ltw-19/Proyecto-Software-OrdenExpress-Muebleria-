<script setup>
import { ref, onMounted } from 'vue'
import { pedidoServicio, extraerMensajeError } from '../servicios/pedidoServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const pedidos = ref([])
const cargando = ref(false)
const eliminandoId = ref(null)
const mensajeError = ref('')
const mensajeExito = ref('')

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
    mensajeError.value = extraerMensajeError(error, 'No se pudieron cargar tus pedidos.')
    pedidos.value = []
  } finally {
    cargando.value = false
  }
}

async function eliminarPedido(pedido) {
  if (pedido.estado !== 'PENDIENTE') {
    mensajeError.value = 'Solo puedes eliminar pedidos en estado PENDIENTE.'
    return
  }

  const confirmar = window.confirm(`¿Estás seguro de que deseas eliminar el pedido ${pedido.numero}? Esta acción no se puede deshacer.`)
  if (!confirmar) return

  eliminandoId.value = pedido.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    await pedidoServicio.eliminar(pedido.id)
    mensajeExito.value = `El pedido ${pedido.numero} ha sido eliminado exitosamente.`
    await cargarPedidos()
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al eliminar el pedido.')
  } finally {
    eliminandoId.value = null
  }
}

async function descargarPdf(pedido) {
  try {
    await pedidoServicio.descargarPreorden(pedido.id, pedido.numero)
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al descargar el PDF de la pre-orden.')
  }
}

function abrirWhatsapp(enlace) {
  if (enlace) {
    window.open(enlace, '_blank')
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
  <div class="contenedor vista-pedidos">
    <header class="encabezado-pedidos">
      <div>
        <h1>Mis Pedidos</h1>
        <p>Historial y seguimiento de tus pre-ordenes de muebles en Galería de Casa.</p>
      </div>
      <button class="btn btn-outline" :disabled="cargando" @click="cargarPedidos">
        ↻ Actualizar
      </button>
    </header>

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

    <div v-if="cargando" class="cargando">
      Cargando tus pedidos...
    </div>

    <div v-else-if="pedidos.length === 0" class="tarjeta estado-vacio">
      <div class="icono-vacio">📦</div>
      <h2>No tienes pedidos registrados</h2>
      <p>Cuando armes tu pre-orden desde el carrito, podrás darle seguimiento aquí.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Ir al Catálogo
      </router-link>
    </div>

    <div v-else class="lista-pedidos">
      <article
        v-for="pedido in pedidos"
        :key="pedido.id"
        class="tarjeta tarjeta-pedido"
      >
        <!-- Cabecera de la tarjeta del pedido -->
        <div class="cabecera-pedido">
          <div class="identificacion-pedido">
            <h2 class="numero-pedido">{{ pedido.numero }}</h2>
            <span class="fecha-pedido">{{ formatearFecha(pedido.timestamp) }}</span>
          </div>

          <div class="estado-pedido-contenedor">
            <span
              :class="[
                'insignia-estado',
                `estado-${pedido.estado?.toLowerCase()}`
              ]"
            >
              {{ pedido.estado }}
            </span>
          </div>
        </div>

        <!-- Tabla de ítems del pedido -->
        <div class="contenedor-items-pedido">
          <table class="tabla-items-pedido">
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
              <tr v-for="item in pedido.items" :key="item.idMueble">
                <td class="codigo-td">{{ item.codigo }}</td>
                <td class="nombre-td">{{ item.nombre }}</td>
                <td class="cant-td">{{ item.cantidad }}</td>
                <td class="precio-td">
                  $ {{ formatoMoneda(item.precioUnitarioUsd) }}
                </td>
                <td class="subtotal-td">
                  <strong>$ {{ formatoMoneda(item.subtotalUsd) }}</strong>
                  <br />
                  <small>Bs. {{ formatoMoneda(item.subtotalBs) }}</small>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Sección de Entrega y Totales -->
        <div class="detalles-entrega-totales">
          <div class="info-entrega">
            <h4>Datos de Entrega:</h4>
            <p><strong>Destinatario:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</p>
            <p><strong>Teléfono:</strong> {{ pedido.entrega?.telefono }}</p>
            <p><strong>Dirección:</strong> {{ pedido.entrega?.direccion }}</p>
          </div>

          <div class="bloque-totales">
            <div class="fila-total-usd">
              <span>Total USD:</span>
              <strong>$ {{ formatoMoneda(pedido.totalUsd) }}</strong>
            </div>
            <div class="fila-total-bs">
              <span>Total en Bs:</span>
              <strong>Bs. {{ formatoMoneda(pedido.totalBs) }}</strong>
            </div>
            <small class="tasa-congelada">
              Tasa congelada: 1 USD = Bs. {{ formatoMoneda(pedido.tasaBcv) }}
            </small>
          </div>
        </div>

        <!-- Acciones del Pedido -->
        <div class="pie-acciones-pedido">
          <div class="acciones-documento">
            <button
              type="button"
              class="btn btn-outline btn-sm"
              @click="descargarPdf(pedido)"
            >
              📄 Pre-orden PDF
            </button>
            <button
              type="button"
              class="btn btn-whatsapp btn-sm"
              @click="abrirWhatsapp(pedido.enlaceWhatsapp)"
            >
              📱 Enviar a WhatsApp
            </button>
          </div>

          <!-- REGLA DE NEGOCIO: El CLIENTE solo puede eliminar pedidos en estado PENDIENTE -->
          <button
            v-if="pedido.estado === 'PENDIENTE'"
            type="button"
            class="btn btn-peligro btn-sm"
            :disabled="eliminandoId === pedido.id"
            @click="eliminarPedido(pedido)"
          >
            🗑️ Eliminar Pedido
          </button>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.vista-pedidos {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.encabezado-pedidos {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.cargando {
  text-align: center;
  padding: 3rem;
  color: var(--color-texto-secundario);
}

.estado-vacio {
  text-align: center;
  padding: 3.5rem 1.5rem;
  max-width: 500px;
  margin: 2rem auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.icono-vacio {
  font-size: 3.5rem;
}

.lista-pedidos {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.tarjeta-pedido {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cabecera-pedido {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-borde);
  padding-bottom: 0.85rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.identificacion-pedido {
  display: flex;
  align-items: baseline;
  gap: 0.85rem;
}

.numero-pedido {
  font-size: 1.35rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0;
}

.fecha-pedido {
  font-size: 0.85rem;
  color: var(--color-texto-mutado);
}

.insignia-estado {
  display: inline-block;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
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

.contenedor-items-pedido {
  overflow-x: auto;
}

.tabla-items-pedido {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.tabla-items-pedido th {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  padding: 0.65rem 0.85rem;
  text-align: left;
  font-weight: 600;
}

.tabla-items-pedido td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid var(--color-borde);
}

.codigo-td {
  font-weight: 600;
  color: var(--color-nogal-secundario);
}

.subtotal-td {
  text-align: right;
}

.detalles-entrega-totales {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1.5rem;
  align-items: flex-start;
  background-color: #FAFAFA;
  padding: 1rem;
  border-radius: var(--radio-control);
}

.info-entrega h4 {
  font-size: 0.9rem;
  margin-bottom: 0.4rem;
  color: var(--color-nogal-primario);
}

.info-entrega p {
  margin-bottom: 0.2rem;
  font-size: 0.85rem;
}

.bloque-totales {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.fila-total-usd {
  display: flex;
  gap: 0.75rem;
  align-items: baseline;
  font-size: 1.15rem;
}

.fila-total-usd strong {
  color: var(--color-nogal-primario);
  font-size: 1.35rem;
}

.fila-total-bs {
  display: flex;
  gap: 0.75rem;
  font-size: 0.95rem;
  color: var(--color-texto-principal);
}

.tasa-congelada {
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
}

.pie-acciones-pedido {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  border-top: 1px solid var(--color-borde);
  padding-top: 0.85rem;
}

.acciones-documento {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.btn-sm {
  padding: 0.45rem 0.85rem;
  font-size: 0.85rem;
}

@media (max-width: 768px) {
  .detalles-entrega-totales {
    grid-template-columns: 1fr;
  }

  .bloque-totales {
    align-items: flex-start;
  }
}
</style>