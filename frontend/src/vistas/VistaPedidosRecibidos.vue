<script setup>
/**
 * VistaPedidosRecibidos.vue
 *
 * Módulo: Pedidos Recibidos / Bandeja Global de Pedidos (ADMIN y GESTOR)
 * Pendiente: Historia a completar por el compañero de equipo (30% restante).
 *
 * REGLAS DE NEGOCIO Y CONTRATO DEL BACKEND (CLAUDE.md):
 * 1. Acceso: ADMIN y GESTOR (ambos consultan la bandeja global con GET /api/pedidos).
 * 2. Visualización:
 *    - Número de pedido (OE-####), fecha, estado (PENDIENTE amarillo, APROBADO verde, RECHAZADO rojo).
 *    - Total en USD y en Bs. formateados con Intl.NumberFormat('es-VE').
 *    - Detalle del pedido (GET /api/pedidos/{id}): datos de entrega, tasa congelada, ítems.
 * 3. Permisos de acciones (ESTRICTO):
 *    - GESTOR:
 *      * Aprobar pedido: PUT /api/pedidos/{id}/aprobar (SOLO si está PENDIENTE; descuenta stock).
 *      * Rechazar pedido: PUT /api/pedidos/{id}/rechazar (SOLO si está PENDIENTE; no toca stock).
 *      * Eliminar pedido: DELETE /api/pedidos/{id} (cualquier estado, no devuelve stock).
 *    - ADMIN:
 *      * SOLO LECTURA. No puede aprobar, rechazar ni eliminar pedidos (no renderizar botones).
 */
import { ref, onMounted } from 'vue'
import { sesion } from '../estado/sesion.js'
import { pedidoServicio } from '../servicios/pedidoServicio.js'
import { extraerMensajeError } from '../servicios/muebleServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const pedidos = ref([])
const cargando = ref(true)
const mensajeError = ref('')
const mensajeExito = ref('')

onMounted(async () => {
  await cargarBandeja()
})

async function cargarBandeja() {
  cargando.value = true
  mensajeError.value = ''
  try {
    const lista = await pedidoServicio.consultarPedidos()
    pedidos.value = Array.isArray(lista) ? lista : []
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudieron cargar los pedidos recibidos.'
    )
  } finally {
    cargando.value = false
  }
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
</script>

<template>
  <div class="contenedor vista-pedidos-recibidos">
    <header class="encabezado-pedidos">
      <div>
        <h1 class="titulo-pedidos">Bandeja de Pedidos Recibidos</h1>
        <p class="subtitulo-pedidos">
          Gestión y seguimiento global de pedidos para Administradores y Gestores de Venta.
        </p>
      </div>
      <button type="button" class="btn btn-outline" :disabled="cargando" @click="cargarBandeja">
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

    <!-- Nota informativa para el equipo -->
    <div class="tarjeta aviso-tarea-pendiente">
      <div class="icono-aviso" aria-hidden="true">🛠️</div>
      <div class="contenido-aviso">
        <h3>Módulo de Gestión de Pedidos Recibidos (Asignado a compañero de equipo)</h3>
        <p>
          Este módulo ya cuenta con los servicios integrados en <code>pedidoServicio.js</code> (<code>consultarPedidos</code>,
          <code>aprobarPedido</code>, <code>rechazarPedido</code>, <code>eliminarPedido</code>).
          Aquí se listan actualmente <strong>{{ pedidos.length }}</strong> pedidos recibidos del backend.
        </p>
      </div>
    </div>

    <div v-if="cargando" class="estado-carga">
      <div class="spinner" aria-hidden="true"></div>
      <p>Cargando bandeja de pedidos...</p>
    </div>

    <div v-else-if="pedidos.length === 0" class="tarjeta estado-vacio">
      <div class="icono-vacio" aria-hidden="true">📥</div>
      <h2>No hay pedidos recibidos</h2>
      <p>Aún no se han registrado pedidos en el sistema.</p>
    </div>

    <!-- Lista básica preliminar conectada a los datos reales -->
    <div v-else class="lista-preliminar-pedidos">
      <article
        v-for="pedido in pedidos"
        :key="pedido.id"
        class="tarjeta tarjeta-pedido-admin"
      >
        <div class="fila-cabecera">
          <span class="numero">{{ pedido.numero }}</span>
          <span :class="['insignia-estado', `estado-${pedido.estado?.toLowerCase()}`]">
            {{ pedido.estado }}
          </span>
        </div>
        <p class="cliente"><strong>Cliente:</strong> {{ pedido.entrega?.nombre }} {{ pedido.entrega?.apellido }} ({{ pedido.entrega?.telefono }})</p>
        <p class="direccion"><strong>Dirección:</strong> {{ pedido.entrega?.direccion }}</p>
        <div class="totales">
          <span><strong>Total USD:</strong> {{ formatearUsd(pedido.totalUsd) }}</span>
          <span><strong>Total Bs:</strong> {{ formatearBs(pedido.totalBs) }}</span>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.vista-pedidos-recibidos {
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
}

.subtitulo-pedidos {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
}

.aviso-tarea-pendiente {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: var(--color-nogal-fondo);
  border-left: 4px solid var(--color-nogal-primario);
  padding: 1.25rem;
  margin-bottom: 1.5rem;
}

.icono-aviso {
  font-size: 2rem;
}

.contenido-aviso h3 {
  font-size: 1.05rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.25rem;
}

.contenido-aviso p {
  font-size: 0.88rem;
  color: var(--color-texto-secundario);
  margin: 0;
}

.estado-carga, .estado-vacio {
  text-align: center;
  padding: 3rem 1rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-borde);
  border-top-color: var(--color-nogal-primario);
  border-radius: 50%;
  animation: girar 0.8s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes girar {
  to { transform: rotate(360deg); }
}

.lista-preliminar-pedidos {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.tarjeta-pedido-admin {
  padding: 1.25rem;
}

.fila-cabecera {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.numero {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
}

.insignia-estado {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 12px;
}

.estado-pendiente {
  background-color: #FFF8E1;
  color: #F57F17;
}

.estado-aprobado {
  background-color: #E8F5E9;
  color: #2E7D32;
}

.estado-rechazado {
  background-color: #FFEBEE;
  color: #C62828;
}

.totales {
  display: flex;
  gap: 1.5rem;
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-borde);
}
</style>
