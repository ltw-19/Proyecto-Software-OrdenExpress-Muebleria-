<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { sesion } from '../estado/sesion.js'
import { muebleServicio, extraerMensajeError } from '../servicios/muebleServicio.js'
import { carritoServicio } from '../servicios/carritoServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const route = useRoute()

const mueble = ref(null)
const cargando = ref(false)
const agregando = ref(false)
const cantidad = ref(1)
const imagenConError = ref(false)
const mensajeError = ref('')
const mensajeExito = ref('')

onMounted(async () => {
  await cargarMueble()
})

watch(
  () => route.params.id,
  (nuevoId) => {
    if (nuevoId) {
      cantidad.value = 1
      mensajeExito.value = ''
      cargarMueble()
    }
  }
)

async function cargarMueble() {
  cargando.value = true
  mensajeError.value = ''
  imagenConError.value = false

  try {
    mueble.value = await muebleServicio.obtenerPorId(route.params.id)
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo encontrar el mueble solicitado.')
    mueble.value = null
  } finally {
    cargando.value = false
  }
}

const precioUsdFormateado = computed(() => {
  if (!mueble.value || mueble.value.precioUsd == null || isNaN(mueble.value.precioUsd)) {
    return '$ 0,00'
  }
  return '$ ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(mueble.value.precioUsd)
})

const precioBsFormateado = computed(() => {
  if (!mueble.value || mueble.value.precioBs == null || isNaN(mueble.value.precioBs)) {
    return 'Bs. —'
  }
  return 'Bs. ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(mueble.value.precioBs)
})

const dimensionesFormateadas = computed(() => {
  const d = mueble.value?.dimensiones
  if (!d) return ''
  if (typeof d === 'string') return d
  const ancho = d.ancho ?? '—'
  const prof = d.profundidad ?? '—'
  const alto = d.alto ?? '—'
  return `${ancho} × ${prof} × ${alto} cm`
})

const estaDisponible = computed(() => {
  if (!mueble.value) return false
  if (mueble.value.disponible !== undefined) {
    return Boolean(mueble.value.disponible)
  }
  if (mueble.value.unidades !== undefined && mueble.value.unidades !== null) {
    return mueble.value.unidades > 0
  }
  return true
})

const maximoCantidad = computed(() => {
  if (mueble.value?.unidades !== undefined && mueble.value.unidades !== null && mueble.value.unidades > 0) {
    return mueble.value.unidades
  }
  return 99
})

async function anadirAlCarrito() {
  if (!mueble.value || !estaDisponible.value) return

  const num = Number(cantidad.value)
  if (!Number.isInteger(num) || num < 1) {
    mensajeError.value = 'Ingresa una cantidad entera válida (mínimo 1 unidad).'
    return
  }
  if (num > maximoCantidad.value) {
    mensajeError.value = `La cantidad máxima permitida es ${maximoCantidad.value} unidad(es).`
    return
  }

  agregando.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    await carritoServicio.agregarItem(mueble.value.id, num)
    mensajeExito.value = `¡Se agregó "${mueble.value.nombre}" (${num} unid.) a tu carrito!`
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo añadir el mueble al carrito.')
  } finally {
    agregando.value = false
  }
}
</script>

<template>
  <div class="contenedor vista-detalle-mueble">
    <div class="barra-superior">
      <router-link to="/catalogo" class="btn btn-outline btn-sm">
        ← Volver al Catálogo
      </router-link>

      <nav v-if="mueble" class="miga-pan" aria-label="Ruta de navegación">
        <router-link to="/catalogo">Catálogo</router-link>
        <span class="separador">/</span>
        <span>{{ mueble.categoria }}</span>
        <span class="separador">/</span>
        <span class="actual">{{ mueble.nombre }}</span>
      </nav>
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

    <!-- Estado de Carga -->
    <div v-if="cargando" class="estado-carga">
      <div class="spinner" aria-hidden="true"></div>
      <p>Cargando información del mueble...</p>
    </div>

    <!-- Estado No Encontrado -->
    <div v-else-if="!mueble" class="tarjeta estado-vacio">
      <div class="icono-vacio">🪑</div>
      <h2>Mueble no encontrado</h2>
      <p>La pieza que buscas no existe o ya no forma parte del catálogo.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Explorar el Catálogo
      </router-link>
    </div>

    <!-- Ficha Completa del Mueble -->
    <article v-else class="tarjeta ficha-mueble">
      <div class="cuadricula-ficha">
        <!-- Columna Izquierda: Imagen Ampliada -->
        <div class="columna-imagen">
          <div class="marco-imagen">
            <img
              v-if="mueble.urlFoto && !imagenConError"
              :src="mueble.urlFoto"
              :alt="mueble.nombre"
              class="imagen-grande"
              @error="imagenConError = true"
            />
            <div v-else class="imagen-respaldo">
              <svg
                class="icono-mueble"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                <path d="M4 11h16a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z" />
                <line x1="6" y1="21" x2="6" y2="23" />
                <line x1="18" y1="21" x2="18" y2="23" />
              </svg>
              <span>Galería de Casa</span>
            </div>

            <span class="insignia-codigo">{{ mueble.codigo || 'M-000' }}</span>

            <div class="insignia-estado-flotante">
              <span
                v-if="sesion.esAdmin.value || sesion.esGestor.value"
                :class="[
                  'insignia-stock',
                  mueble.unidades > 0 ? 'stock-disponible' : 'stock-agotado'
                ]"
              >
                Stock: {{ mueble.unidades ?? (estaDisponible ? 'Disponible' : 0) }} uds.
              </span>
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
        </div>

        <!-- Columna Derecha: Información Detallada -->
        <div class="columna-detalles">
          <span class="etiqueta-categoria">{{ mueble.categoria }}</span>
          <h1 class="titulo-mueble">{{ mueble.nombre }}</h1>

          <p class="descripcion-completa">{{ mueble.descripcion }}</p>

          <!-- Especificaciones Técnicas -->
          <div class="bloque-especificaciones">
            <h3>Especificaciones</h3>
            <div class="lista-specs">
              <div class="fila-spec">
                <span class="label-spec">Código de referencia:</span>
                <strong>{{ mueble.codigo }}</strong>
              </div>
              <div v-if="mueble.material" class="fila-spec">
                <span class="label-spec">Materiales:</span>
                <strong>{{ mueble.material }}</strong>
              </div>
              <div v-if="dimensionesFormateadas" class="fila-spec">
                <span class="label-spec">Dimensiones (Ancho × Prof. × Alto):</span>
                <strong>{{ dimensionesFormateadas }}</strong>
              </div>
              <div class="fila-spec">
                <span class="label-spec">Estado:</span>
                <strong v-if="sesion.esAdmin.value || sesion.esGestor.value">
                  {{ mueble.unidades }} unidad(es) en inventario
                </strong>
                <strong v-else>
                  {{ estaDisponible ? 'Disponible para entrega' : 'Temporalmente agotado' }}
                </strong>
              </div>
            </div>
          </div>

          <!-- Bloque Bimonetario -->
          <div class="bloque-precios">
            <div class="precio-principal">
              <span class="cifra-usd">{{ precioUsdFormateado }}</span>
              <span class="moneda-usd">USD</span>
            </div>
            <div class="precio-secundario">
              <span class="cifra-bs">{{ precioBsFormateado }}</span>
              <span class="nota-bcv">Calculado a tasa oficial del BCV</span>
            </div>
          </div>

          <!-- Acciones para CLIENTE -->
          <div v-if="sesion.esCliente.value" class="zona-compra-cliente">
            <div class="control-cantidad">
              <label for="cantidad-detalle" class="etiqueta">Cantidad:</label>
              <input
                id="cantidad-detalle"
                v-model.number="cantidad"
                type="number"
                min="1"
                :max="maximoCantidad"
                class="campo-texto input-cantidad"
                :disabled="!estaDisponible || agregando"
              />
            </div>

            <button
              type="button"
              class="btn btn-primario btn-agregar-grande"
              :disabled="!estaDisponible || agregando"
              @click="anadirAlCarrito"
            >
              <span v-if="agregando">Añadiendo...</span>
              <span v-else-if="estaDisponible">Añadir al carrito</span>
              <span v-else>Agotado</span>
            </button>
          </div>

          <div v-if="mensajeExito && sesion.esCliente.value" class="acceso-rapido-carrito">
            <router-link to="/carrito" class="btn btn-secundario btn-sm">
              🛒 Ir a mi Carrito
            </router-link>
          </div>

          <!-- Acciones para ADMIN / GESTOR -->
          <div v-else-if="sesion.esAdmin.value || sesion.esGestor.value" class="zona-admin">
            <p class="nota-admin">
              Vista de consulta administrativa. Puedes gestionar el registro y unidades desde el Inventario.
            </p>
            <router-link to="/inventario" class="btn btn-secundario btn-sm">
              📦 Ir al Inventario
            </router-link>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>

<style scoped>
.vista-detalle-mueble {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.btn-sm {
  padding: 0.45rem 0.9rem;
  font-size: 0.85rem;
}

.btn:hover {
  text-decoration: none;
}

.barra-superior {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.miga-pan {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-texto-mutado);
}

.miga-pan .separador {
  color: var(--color-borde);
}

.miga-pan .actual {
  color: var(--color-nogal-primario);
  font-weight: 600;
}

.estado-carga, .estado-vacio {
  text-align: center;
  padding: 4rem 1.5rem;
  color: var(--color-texto-secundario);
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
  to { transform: rotate(360deg); }
}

.icono-vacio {
  font-size: 3.5rem;
}

.ficha-mueble {
  padding: 2rem;
}

.cuadricula-ficha {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 2.5rem;
  align-items: start;
}

.marco-imagen {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background-color: var(--color-nogal-fondo);
  border-radius: var(--radio-tarjeta);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-borde);
}

.imagen-grande {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.imagen-respaldo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-nogal-secundario);
}

.icono-mueble {
  width: 64px;
  height: 64px;
}

.insignia-codigo {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background-color: rgba(33, 37, 41, 0.8);
  color: #FFFFFF;
  padding: 0.3rem 0.75rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  backdrop-filter: blur(4px);
}

.insignia-estado-flotante {
  position: absolute;
  top: 1rem;
  right: 1rem;
}

.insignia-stock,
.insignia-disponibilidad {
  display: inline-block;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.stock-disponible, .disponible {
  background-color: rgba(46, 125, 50, 0.92);
  color: #FFFFFF;
}

.stock-agotado, .agotado {
  background-color: rgba(198, 40, 40, 0.92);
  color: #FFFFFF;
}

.etiqueta-categoria {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: var(--color-nogal-secundario);
  margin-bottom: 0.35rem;
}

.titulo-mueble {
  font-size: 2rem;
  color: var(--color-nogal-primario);
  margin-bottom: 1rem;
}

.descripcion-completa {
  font-size: 1rem;
  line-height: 1.7;
  color: var(--color-texto-principal);
  margin-bottom: 1.5rem;
}

.bloque-especificaciones {
  background-color: #FAFAFA;
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  padding: 1.15rem;
  margin-bottom: 1.5rem;
}

.bloque-especificaciones h3 {
  font-size: 0.95rem;
  margin-bottom: 0.75rem;
  color: var(--color-nogal-primario);
}

.lista-specs {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.fila-spec {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  font-size: 0.9rem;
  border-bottom: 1px dashed var(--color-borde);
  padding-bottom: 0.4rem;
}

.fila-spec:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.label-spec {
  color: var(--color-texto-secundario);
}

.bloque-precios {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  background-color: var(--color-nogal-fondo);
  padding: 1.15rem 1.35rem;
  border-radius: var(--radio-control);
  margin-bottom: 1.5rem;
}

.precio-principal {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}

.cifra-usd {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-nogal-primario);
}

.moneda-usd {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-nogal-secundario);
}

.precio-secundario {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.cifra-bs {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-texto-principal);
}

.nota-bcv {
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
}

.zona-compra-cliente {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.control-cantidad {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.input-cantidad {
  width: 75px;
  text-align: center;
  font-weight: 700;
}

.btn-agregar-grande {
  flex: 1;
  padding: 0.85rem 1.5rem;
  font-size: 1rem;
}

.acceso-rapido-carrito {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}

.zona-admin {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-borde);
}

.nota-admin {
  font-size: 0.85rem;
  color: var(--color-texto-mutado);
  margin-bottom: 0;
  flex: 1;
}

@media (max-width: 860px) {
  .ficha-mueble {
    padding: 1.25rem;
  }

  .cuadricula-ficha {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .titulo-mueble {
    font-size: 1.65rem;
  }

  .zona-compra-cliente {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>