<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sesion } from '../estado/sesion.js'
import { pedidoServicio, extraerMensajeError } from '../servicios/pedidoServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const route = useRoute()
const router = useRouter()

const pedido = ref(null)
const cargando = ref(false)
const procesando = ref(false)
const mensajeError = ref('')
const mensajeExito = ref('')

onMounted(async () => {
  await cargarDetalle()
})

async function cargarDetalle() {
  cargando.value = true
  mensajeError.value = ''
  try {
    pedido.value = await pedidoServicio.consultarDetalle(route.params.id)
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo cargar el detalle del pedido.')
    pedido.value = null
  } finally {
    cargando.value = false
  }
}

async function aprobar() {
  if (!pedido.value || pedido.value.estado !== 'PENDIENTE') return
  const confirmar = window.confirm(`¿Aprobar el pedido ${pedido.value.numero}? Se descontará el stock de los muebles del inventario.`)
  if (!confirmar) return

  procesando.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    pedido.value = await pedidoServicio.aprobar(pedido.value.id)
    mensajeExito.value = `El pedido ${pedido.value.numero} ha sido APROBADO y el stock fue descontado.`
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo aprobar el pedido.')
  } finally {
    procesando.value = false
  }
}

async function rechazar() {
  if (!pedido.value || pedido.value.estado !== 'PENDIENTE') return
  const confirmar = window.confirm(`¿Rechazar el pedido ${pedido.value.numero}? El estado cambiará a RECHAZADO.`)
  if (!confirmar) return

  procesando.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    pedido.value = await pedidoServicio.rechazar(pedido.value.id)
    mensajeExito.value = `El pedido ${pedido.value.numero} ha sido RECHAZADO.`
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo rechazar el pedido.')
  } finally {
    procesando.value = false
  }
}

async function eliminar() {
  if (!pedido.value) return
  const confirmar = window.confirm(`¿Eliminar definitivamente el pedido ${pedido.value.numero}? Esta acción no devuelve stock ni se puede deshacer.`)
  if (!confirmar) return

  procesando.value = true
  mensajeError.value = ''

  try {
    await pedidoServicio.eliminar(pedido.value.id)
    router.push('/pedidos')
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo eliminar el pedido.')
    procesando.value = false
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
      dateStyle: 'long',
      timeStyle: 'short'
    }).format(fecha)
  } catch {
    return isoString
  }
}
</script>

<template>
  <div class="contenedor vista-detalle-pedido">
    <div class="barra-volver">
      <router-link to="/pedidos" class="btn btn-outline btn-sm">
        ← Volver a Pedidos Recibidos
      </router-link>
    </div>

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
      Cargando detalle del pedido...
    </div>

    <div v-else-if="!pedido" class="tarjeta estado-vacio">
      <h2>Pedido no encontrado</h2>
      <p>El pedido solicitado no existe o no pudo ser cargado.</p>
      <router-link to="/pedidos" class="btn btn-primario">
        Ir a Pedidos Recibidos
      </router-link>
    </div>

    <div v-else class="contenido-detalle">
      <!-- Encabezado con número, fecha y estado -->
      <div class="tarjeta cabecera-detalle">
        <div class="info-principal">
          <div class="fila-titulo">
            <h1>Pedido {{ pedido.numero }}</h1>
            <span
              :class="[
                'insignia-estado',
                `estado-${pedido.estado?.toLowerCase()}`
              ]"
            >
              {{ pedido.estado }}
            </span>
          </div>
          <p class="fecha-creacion">Registrado el {{ formatearFecha(pedido.timestamp) }}</p>
        </div>

        <!-- Botones exclusivos para el GESTOR DE VENTAS -->
        <div v-if="sesion.esGestor.value" class="acciones-gestor">
          <button
            v-if="pedido.estado === 'PENDIENTE'"
            type="button"
            class="btn btn-aprobar"
            :disabled="procesando"
            @click="aprobar"
          >
            ✓ Aprobar Pedido
          </button>
          <button
            v-if="pedido.estado === 'PENDIENTE'"
            type="button"
            class="btn btn-rechazar"
            :disabled="procesando"
            @click="rechazar"
          >
            ✗ Rechazar Pedido
          </button>
          <button
            type="button"
            class="btn btn-peligro"
            :disabled="procesando"
            @click="eliminar"
          >
            🗑️ Eliminar
          </button>
        </div>

        <!-- Aviso de solo lectura para ADMIN -->
        <div v-else class="aviso-admin">
          <span class="insignia insignia-admin">MODO SOLO LECTURA</span>
        </div>
      </div>

      <!-- Cuadrícula con Datos de Entrega y Resumen Económico -->
      <div class="cuadricula-info">
        <section class="tarjeta tarjeta-entrega">
          <h2>Datos de Entrega</h2>
          <div class="datos-lista">
            <p><strong>Cliente:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }}</p>
            <p><strong>Teléfono:</strong> {{ pedido.entrega?.telefono }}</p>
            <p><strong>Dirección:</strong> {{ pedido.entrega?.direccion }}</p>
          </div>
        </section>

        <section class="tarjeta tarjeta-totales">
          <h2>Resumen Financiero</h2>
          <div class="datos-lista">
            <div class="linea-monto">
              <span>Total USD:</span>
              <strong class="total-usd">$ {{ formatoMoneda(pedido.totalUsd) }}</strong>
            </div>
            <div class="linea-monto">
              <span>Total Bolívares:</span>
              <strong class="total-bs">Bs. {{ formatoMoneda(pedido.totalBs) }}</strong>
            </div>
            <p class="nota-tasa-congelada">
              Tasa BCV congelada al registrar: <strong>1 USD = Bs. {{ formatoMoneda(pedido.tasaBcv) }}</strong>
            </p>
          </div>
        </section>
      </div>

      <!-- Listado de Ítems (Sin edición según reglas) -->
      <section class="tarjeta seccion-items">
        <h2>Muebles del Pedido ({{ pedido.items?.length || 0 }})</h2>
        <p class="nota-sin-edicion">Los ítems y montos congelados son de solo lectura.</p>

        <!-- Tabla para Escritorio -->
        <table class="tabla-items">
          <thead>
            <tr>
              <th>Código</th>
              <th>Mueble</th>
              <th>Cantidad</th>
              <th>Precio Unit. USD</th>
              <th>Precio Unit. Bs</th>
              <th>Subtotal USD</th>
              <th>Subtotal Bs</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pedido.items" :key="item.idMueble">
              <td class="codigo-col">{{ item.codigo }}</td>
              <td><strong>{{ item.nombre }}</strong></td>
              <td>{{ item.cantidad }}</td>
              <td>$ {{ formatoMoneda(item.precioUnitarioUsd) }}</td>
              <td>Bs. {{ formatoMoneda(item.precioUnitarioBs) }}</td>
              <td><strong>$ {{ formatoMoneda(item.subtotalUsd) }}</strong></td>
              <td>Bs. {{ formatoMoneda(item.subtotalBs) }}</td>
            </tr>
          </tbody>
        </table>

        <!-- Tarjetas para Móvil -->
        <div class="lista-items-movil">
          <div v-for="item in pedido.items" :key="item.idMueble" class="tarjeta-item-movil">
            <div class="cabecera-item-movil">
              <span class="codigo-col">{{ item.codigo }}</span>
              <strong>{{ item.nombre }}</strong>
            </div>
            <p><strong>Cantidad:</strong> {{ item.cantidad }} uds.</p>
            <p><strong>Unitario:</strong> $ {{ formatoMoneda(item.precioUnitarioUsd) }} / Bs. {{ formatoMoneda(item.precioUnitarioBs) }}</p>
            <p class="subtotal-movil">
              <strong>Subtotal:</strong> $ {{ formatoMoneda(item.subtotalUsd) }} (Bs. {{ formatoMoneda(item.subtotalBs) }})
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.vista-detalle-pedido {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.barra-volver {
  margin-bottom: 1.25rem;
}

.cargando, .estado-vacio {
  text-align: center;
  padding: 3rem;
  color: var(--color-texto-secundario);
}

.contenido-detalle {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.cabecera-detalle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.fila-titulo {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.fila-titulo h1 {
  margin-bottom: 0;
}

.fecha-creacion {
  margin-top: 0.35rem;
  margin-bottom: 0;
  font-size: 0.9rem;
  color: var(--color-texto-mutado);
}

.insignia-estado {
  display: inline-block;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  font-size: 0.8rem;
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

.acciones-gestor {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn-aprobar {
  background-color: var(--color-exito);
  color: #FFFFFF;
}

.btn-aprobar:hover:not(:disabled) {
  background-color: #1B5E20;
}

.btn-rechazar {
  background-color: var(--color-aviso);
  color: #FFFFFF;
}

.btn-rechazar:hover:not(:disabled) {
  background-color: #BF360C;
}

.cuadricula-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.datos-lista {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.datos-lista p {
  margin-bottom: 0;
  font-size: 0.95rem;
}

.linea-monto {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 1rem;
}

.total-usd {
  font-size: 1.4rem;
  color: var(--color-nogal-primario);
}

.total-bs {
  font-size: 1.15rem;
  color: var(--color-texto-principal);
}

.nota-tasa-congelada {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-borde);
  font-size: 0.85rem;
  color: var(--color-texto-mutado);
}

.nota-sin-edicion {
  font-size: 0.85rem;
  color: var(--color-texto-mutado);
  margin-bottom: 1rem;
}

.tabla-items {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.tabla-items th {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
}

.tabla-items td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-borde);
}

.codigo-col {
  font-weight: 700;
  color: var(--color-nogal-secundario);
}

.lista-items-movil {
  display: none;
  flex-direction: column;
  gap: 0.85rem;
}

.tarjeta-item-movil {
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  padding: 0.9rem;
  background-color: #FAFAFA;
}

.cabecera-item-movil {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}

.tarjeta-item-movil p {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.subtotal-movil {
  margin-top: 0.35rem;
  color: var(--color-nogal-primario);
}

@media (max-width: 768px) {
  .cuadricula-info {
    grid-template-columns: 1fr;
  }

  .tabla-items {
    display: none;
  }

  .lista-items-movil {
    display: flex;
  }
}
</style>