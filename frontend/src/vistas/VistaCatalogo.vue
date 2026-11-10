<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { sesion } from '../estado/sesion.js'
import { muebleServicio, extraerMensajeError } from '../servicios/muebleServicio.js'
import { carritoServicio } from '../servicios/carritoServicio.js'
import TarjetaMueble from '../componentes/TarjetaMueble.vue'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()

const muebles = ref([])
const categorias = ref([])
const busqueda = ref('')
const categoriaSeleccionada = ref('TODAS')
const cargando = ref(false)
const mensajeError = ref('')
const mensajeAviso = ref('')
let temporizadorBusqueda = null

onMounted(async () => {
  await Promise.all([
    cargarCategorias(),
    cargarMuebles()
  ])
})

// Búsqueda con debounce para mejorar la experiencia de usuario
watch(busqueda, () => {
  if (temporizadorBusqueda) clearTimeout(temporizadorBusqueda)
  temporizadorBusqueda = setTimeout(() => {
    cargarMuebles()
  }, 350)
})

// Cambio inmediato al cambiar de categoría
watch(categoriaSeleccionada, () => {
  cargarMuebles()
})

async function cargarCategorias() {
  try {
    const lista = await muebleServicio.obtenerCategorias()
    categorias.value = Array.isArray(lista) ? lista : []
  } catch (error) {
    // Si el backend aún no tiene el endpoint de categorías, no bloqueamos la vista
    categorias.value = []
  }
}

async function cargarMuebles() {
  cargando.value = true
  mensajeError.value = ''

  try {
    const resultado = await muebleServicio.obtenerMuebles({
      busqueda: busqueda.value,
      categoria: categoriaSeleccionada.value
    })
    muebles.value = Array.isArray(resultado) ? resultado : []
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo cargar el catálogo de muebles. Por favor intenta nuevamente.'
    )
    muebles.value = []
  } finally {
    cargando.value = false
  }
}

function limpiarFiltros() {
  busqueda.value = ''
  categoriaSeleccionada.value = 'TODAS'
  cargarMuebles()
}

const mensajeExito = ref('')

async function manejarAnadirCarrito({ mueble, cantidad }) {
  mensajeError.value = ''
  mensajeAviso.value = ''
  mensajeExito.value = ''

  try {
    await carritoServicio.agregarItem(mueble.id, cantidad)
    mensajeExito.value = `¡Se agregó "${mueble.nombre}" (${cantidad} unid.) a tu carrito con éxito!`
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo añadir el mueble al carrito.')
  }
}
</script>

<template>
  <div class="contenedor vista-catalogo">
    <!-- Encabezado del Catálogo -->
    <header class="encabezado-catalogo">
      <div class="informacion-encabezado">
        <h1 class="titulo-catalogo">Catálogo de Muebles</h1>
        <p class="subtitulo-catalogo">
          Explora la colección exclusiva de Galería de Casa en madera de nogal y acabados cálidos.
        </p>
      </div>

      <!-- Enlace directo para ADMIN y GESTOR a la vista de inventario -->
      <div v-if="sesion.esAdmin.value || sesion.esGestor.value" class="acciones-encabezado">
        <router-link to="/inventario" class="btn btn-secundario btn-acceso-inventario">
          📋 Gestionar Inventario
        </router-link>
      </div>
    </header>

    <!-- Alertas y Avisos del Sistema -->
    <AlertaMensaje
      v-if="mensajeExito"
      tipo="exito"
      :mensaje="mensajeExito"
      @cerrar="mensajeExito = ''"
    />

    <AlertaMensaje
      v-if="mensajeError"
      tipo="error"
      :mensaje="mensajeError"
      @cerrar="mensajeError = ''"
    />

    <AlertaMensaje
      v-if="mensajeAviso"
      tipo="aviso"
      :mensaje="mensajeAviso"
      @cerrar="mensajeAviso = ''"
    />

    <!-- Barra de Filtros y Búsqueda -->
    <section class="barra-filtros tarjeta" aria-label="Filtros del catálogo">
      <div class="grupo-busqueda">
        <span class="icono-busqueda" aria-hidden="true">🔍</span>
        <input
          v-model="busqueda"
          type="search"
          class="campo-texto input-busqueda"
          placeholder="Buscar por nombre, descripción o material..."
          aria-label="Buscar muebles"
        />
        <button
          v-if="busqueda"
          type="button"
          class="btn-limpiar-busqueda"
          aria-label="Limpiar búsqueda"
          @click="busqueda = ''"
        >
          &times;
        </button>
      </div>

      <div class="grupo-categoria">
        <label for="filtro-categoria" class="etiqueta-filtro">Categoría:</label>
        <select
          id="filtro-categoria"
          v-model="categoriaSeleccionada"
          class="campo-selector selector-categoria"
        >
          <option value="TODAS">Todas las categorías</option>
          <option v-for="cat in categorias" :key="cat" :value="cat">
            {{ cat }}
          </option>
        </select>
      </div>

      <button
        v-if="busqueda || categoriaSeleccionada !== 'TODAS'"
        type="button"
        class="btn btn-outline btn-reset-filtros"
        @click="limpiarFiltros"
      >
        Limpiar Filtros
      </button>
    </section>

    <!-- Barra de Estado y Resultados -->
    <div class="barra-resumen">
      <span class="conteo-resultados">
        <template v-if="cargando">Buscando muebles...</template>
        <template v-else>
          Mostrando <strong>{{ muebles.length }}</strong>
          {{ muebles.length === 1 ? 'mueble' : 'muebles' }}
          <span v-if="categoriaSeleccionada !== 'TODAS'">
            en <em>{{ categoriaSeleccionada }}</em>
          </span>
        </template>
      </span>

      <button
        type="button"
        class="btn-recargar"
        :disabled="cargando"
        title="Actualizar catálogo"
        @click="cargarMuebles"
      >
        ↻ Actualizar
      </button>
    </div>

    <!-- Estado de Carga -->
    <div v-if="cargando" class="estado-carga">
      <div class="spinner" aria-hidden="true"></div>
      <p>Cargando muebles disponibles...</p>
    </div>

    <!-- Estado Vacío -->
    <div v-else-if="muebles.length === 0" class="tarjeta estado-vacio">
      <div class="icono-vacio" aria-hidden="true">🛋️</div>
      <h2>No se encontraron muebles</h2>
      <p>
        No hay piezas que coincidan con los criterios de búsqueda o filtros aplicados.
      </p>
      <button
        v-if="busqueda || categoriaSeleccionada !== 'TODAS'"
        type="button"
        class="btn btn-primario"
        @click="limpiarFiltros"
      >
        Ver todos los muebles
      </button>
    </div>

    <!-- Cuadrícula de Muebles -->
    <main v-else class="cuadricula-muebles" aria-label="Lista de muebles">
      <TarjetaMueble
        v-for="mueble in muebles"
        :key="mueble.id"
        :mueble="mueble"
        @anadir-al-carrito="manejarAnadirCarrito"
      />
    </main>
  </div>
</template>

<style scoped>
.vista-catalogo {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.encabezado-catalogo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.titulo-catalogo {
  font-size: 1.85rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.25rem;
}

.subtitulo-catalogo {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
  margin-bottom: 0;
}

.btn-acceso-inventario {
  padding: 0.55rem 1rem;
  font-size: 0.9rem;
}

.barra-filtros {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.grupo-busqueda {
  position: relative;
  flex: 1;
  min-width: 240px;
  display: flex;
  align-items: center;
}

.icono-busqueda {
  position: absolute;
  left: 0.85rem;
  font-size: 0.95rem;
  pointer-events: none;
  opacity: 0.6;
}

.input-busqueda {
  padding-left: 2.35rem;
  padding-right: 2rem;
  width: 100%;
}

.btn-limpiar-busqueda {
  position: absolute;
  right: 0.65rem;
  background: transparent;
  border: none;
  font-size: 1.25rem;
  color: var(--color-texto-mutado);
  cursor: pointer;
  line-height: 1;
}

.btn-limpiar-busqueda:hover {
  color: var(--color-texto-principal);
}

.grupo-categoria {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.etiqueta-filtro {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-texto-principal);
  white-space: nowrap;
}

.selector-categoria {
  min-width: 190px;
}

.btn-reset-filtros {
  padding: 0.65rem 0.95rem;
  font-size: 0.875rem;
  white-space: nowrap;
}

.barra-resumen {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
  padding: 0 0.25rem;
  font-size: 0.9rem;
  color: var(--color-texto-secundario);
}

.conteo-resultados strong {
  color: var(--color-nogal-primario);
}

.btn-recargar {
  background: transparent;
  border: none;
  color: var(--color-nogal-primario);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radio-control);
  transition: background-color 0.2s ease;
}

.btn-recargar:hover:not(:disabled) {
  background-color: var(--color-nogal-fondo);
  text-decoration: underline;
}

.btn-recargar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.estado-carga {
  text-align: center;
  padding: 4rem 1rem;
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
  gap: 0.85rem;
}

.icono-vacio {
  font-size: 3rem;
  line-height: 1;
}

.cuadricula-muebles {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .barra-filtros {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }

  .grupo-categoria {
    flex-direction: column;
    align-items: stretch;
  }

  .selector-categoria {
    width: 100%;
  }

  .btn-reset-filtros {
    width: 100%;
  }

  .cuadricula-muebles {
    grid-template-columns: 1fr;
    gap: 1.1rem;
  }
}
</style>
