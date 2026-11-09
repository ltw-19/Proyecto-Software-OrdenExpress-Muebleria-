<script setup>
import { ref, computed } from 'vue'
import { sesion } from '../estado/sesion.js'

const props = defineProps({
  mueble: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['anadir-al-carrito'])

const cantidad = ref(1)
const imagenConError = ref(false)

// Formateo de precios bimonetarios con estándar es-VE sin calcular montos en frontend
const precioUsdFormateado = computed(() => {
  if (props.mueble.precioUsd == null || isNaN(props.mueble.precioUsd)) {
    return '$ 0,00'
  }
  return '$ ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(props.mueble.precioUsd)
})

const precioBsFormateado = computed(() => {
  if (props.mueble.precioBs == null || isNaN(props.mueble.precioBs)) {
    return 'Bs. —'
  }
  return 'Bs. ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(props.mueble.precioBs)
})

// Dimensiones en formato Ancho × Profundidad × Alto cm según especificación
const dimensionesFormateadas = computed(() => {
  const d = props.mueble.dimensiones
  if (!d) return ''
  if (typeof d === 'string') return d
  const ancho = d.ancho ?? '—'
  const prof = d.profundidad ?? '—'
  const alto = d.alto ?? '—'
  return `${ancho} × ${prof} × ${alto} cm`
})

// Evaluación de disponibilidad (el backend envía 'disponible' booleano o 'unidades')
const estaDisponible = computed(() => {
  if (props.mueble.disponible !== undefined) {
    return Boolean(props.mueble.disponible)
  }
  if (props.mueble.unidades !== undefined && props.mueble.unidades !== null) {
    return props.mueble.unidades > 0
  }
  return true
})

// Límite máximo de cantidad que puede seleccionar el cliente
const maximoCantidad = computed(() => {
  if (props.mueble.unidades !== undefined && props.mueble.unidades !== null && props.mueble.unidades > 0) {
    return props.mueble.unidades
  }
  return 99
})

function manejarErrorImagen() {
  imagenConError.value = true
}

function agregarAlCarrito() {
  if (!estaDisponible.value) return
  const cant = Math.max(1, Math.min(cantidad.value || 1, maximoCantidad.value))
  emit('anadir-al-carrito', {
    mueble: props.mueble,
    cantidad: cant
  })
}
</script>

<template>
  <article class="tarjeta-mueble">
    <!-- Contenedor de Imagen -->
    <div class="contenedor-imagen">
      <img
        v-if="mueble.urlFoto && !imagenConError"
        :src="mueble.urlFoto"
        :alt="mueble.nombre"
        class="foto-mueble"
        loading="lazy"
        @error="manejarErrorImagen"
      />
      <!-- Imagen de reemplazo elegante cuando falla o no hay foto -->
      <div v-else class="foto-reemplazo">
        <svg
          class="icono-mueble"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          <path d="M4 11h16a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z" />
          <line x1="6" y1="21" x2="6" y2="23" />
          <line x1="18" y1="21" x2="18" y2="23" />
        </svg>
        <span class="texto-sin-foto">Galería de Casa</span>
      </div>

      <!-- Insignia de código -->
      <span class="insignia-codigo">{{ mueble.codigo || 'M-000' }}</span>

      <!-- Insignia de disponibilidad / stock -->
      <div class="insignia-estado-flotante">
        <!-- Para ADMIN y GESTOR: ven la cifra exacta de unidades -->
        <span
          v-if="sesion.esAdmin.value || sesion.esGestor.value"
          :class="[
            'insignia-stock',
            mueble.unidades > 0 ? 'stock-disponible' : 'stock-agotado'
          ]"
        >
          Stock: {{ mueble.unidades ?? (estaDisponible ? 'Disponible' : 0) }} uds.
        </span>

        <!-- Para CLIENTE: ve solo 'Disponible' o 'Agotado', NUNCA la cifra numérica -->
        <span
          v-else
          :class="[
            'insignia-disponibilidad',
            estaDisponible ? 'disponible' : 'agotado'
          ]"
        >
          {{ estaDisponible ? 'Disponible' : 'Agotado' }}
        </span>
      </div>
    </div>

    <!-- Cuerpo de la Tarjeta -->
    <div class="cuerpo-tarjeta">
      <div class="categoria-mueble">{{ mueble.categoria }}</div>
      <h3 class="nombre-mueble" :title="mueble.nombre">{{ mueble.nombre }}</h3>

      <p class="descripcion-mueble">{{ mueble.descripcion }}</p>

      <!-- Ficha de especificaciones rápidas -->
      <div class="especificaciones">
        <div v-if="mueble.material" class="item-especificacion">
          <span class="etiqueta-esp">Material:</span>
          <span class="valor-esp">{{ mueble.material }}</span>
        </div>
        <div v-if="dimensionesFormateadas" class="item-especificacion">
          <span class="etiqueta-esp">Dimensiones:</span>
          <span class="valor-esp">{{ dimensionesFormateadas }}</span>
        </div>
      </div>

      <!-- Sección Bimonetaria de Precios -->
      <div class="seccion-precios">
        <div class="precio-principal">
          <span class="monto-usd">{{ precioUsdFormateado }}</span>
          <span class="etiqueta-moneda">USD</span>
        </div>
        <div class="precio-secundario">
          <span class="monto-bs">{{ precioBsFormateado }}</span>
          <span class="nota-tasa">Tasa BCV oficial</span>
        </div>
      </div>

      <!-- Acciones de Compra: Solo para CLIENTES -->
      <div v-if="sesion.esCliente.value" class="acciones-cliente">
        <div class="selector-cantidad-grupo">
          <label :for="`cant-${mueble.id}`" class="etiqueta-cant">Cant:</label>
          <input
            :id="`cant-${mueble.id}`"
            v-model.number="cantidad"
            type="number"
            class="campo-texto campo-cantidad"
            min="1"
            :max="maximoCantidad"
            :disabled="!estaDisponible"
          />
        </div>

        <button
          type="button"
          class="btn btn-primario btn-anadir"
          :disabled="!estaDisponible"
          @click="agregarAlCarrito"
        >
          <span v-if="estaDisponible">Añadir al carrito</span>
          <span v-else>Agotado</span>
        </button>
      </div>

      <!-- Vista administrativa de solo lectura para ADMIN y GESTOR -->
      <div v-else class="acciones-administrativas">
        <span class="texto-admin-info">
          Código de referencia: <strong>{{ mueble.codigo }}</strong>
        </span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.tarjeta-mueble {
  background-color: var(--color-superficie);
  border-radius: var(--radio-tarjeta);
  border: 1px solid var(--color-borde);
  box-shadow: var(--sombra-suave);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.tarjeta-mueble:hover {
  transform: translateY(-3px);
  box-shadow: var(--sombra-elevada);
}

.contenedor-imagen {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  background-color: var(--color-nogal-fondo);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.foto-mueble {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.tarjeta-mueble:hover .foto-mueble {
  transform: scale(1.03);
}

.foto-reemplazo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--color-nogal-secundario);
  padding: 1rem;
}

.icono-mueble {
  width: 48px;
  height: 48px;
  stroke: var(--color-nogal-secundario);
}

.texto-sin-foto {
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.insignia-codigo {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  background-color: rgba(33, 37, 41, 0.75);
  color: #FFFFFF;
  padding: 0.2rem 0.55rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
}

.insignia-estado-flotante {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
}

.insignia-stock,
.insignia-disponibilidad {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.stock-disponible,
.disponible {
  background-color: rgba(46, 125, 50, 0.9);
  color: #FFFFFF;
}

.stock-agotado,
.agotado {
  background-color: rgba(198, 40, 40, 0.9);
  color: #FFFFFF;
}

.cuerpo-tarjeta {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.categoria-mueble {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--color-nogal-secundario);
  letter-spacing: 0.5px;
  margin-bottom: 0.25rem;
}

.nombre-mueble {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-nogal-primario);
  margin-bottom: 0.5rem;
  line-height: 1.3;
}

.descripcion-mueble {
  font-size: 0.875rem;
  color: var(--color-texto-secundario);
  margin-bottom: 0.85rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 2.6em;
}

.especificaciones {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.65rem 0;
  border-top: 1px dashed var(--color-borde);
  border-bottom: 1px dashed var(--color-borde);
  margin-bottom: 0.9rem;
  font-size: 0.8rem;
}

.item-especificacion {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.etiqueta-esp {
  color: var(--color-texto-mutado);
  font-weight: 500;
}

.valor-esp {
  color: var(--color-texto-principal);
  font-weight: 600;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seccion-precios {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: auto;
  margin-bottom: 1rem;
  padding: 0.5rem 0.75rem;
  background-color: var(--color-nogal-fondo);
  border-radius: var(--radio-control);
}

.precio-principal {
  display: flex;
  align-items: baseline;
  gap: 0.3rem;
}

.monto-usd {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
  letter-spacing: -0.5px;
}

.etiqueta-moneda {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-nogal-secundario);
}

.precio-secundario {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.monto-bs {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-texto-principal);
}

.nota-tasa {
  font-size: 0.68rem;
  color: var(--color-texto-mutado);
}

.acciones-cliente {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.selector-cantidad-grupo {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.etiqueta-cant {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-texto-secundario);
}

.campo-cantidad {
  width: 55px;
  padding: 0.5rem 0.35rem;
  text-align: center;
  font-size: 0.9rem;
  font-weight: 600;
}

.btn-anadir {
  flex: 1;
  padding: 0.6rem 0.85rem;
  font-size: 0.875rem;
  white-space: nowrap;
}

.acciones-administrativas {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-borde);
}

.texto-admin-info {
  font-size: 0.8rem;
  color: var(--color-texto-mutado);
}
</style>
