<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { muebleServicio, extraerMensajeError } from '../servicios/muebleServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const muebles = ref([])
const categorias = ref([])
const busqueda = ref('')
const cargando = ref(false)
const guardando = ref(false)
const modalRegistroAbierto = ref(false)

const mensajeError = ref('')
const mensajeExito = ref('')

// Formulario de registro de mueble
const formulario = ref({
  nombre: '',
  categoria: '',
  otraCategoria: '',
  descripcion: '',
  ancho: '',
  alto: '',
  profundidad: '',
  material: '',
  urlFoto: '',
  precioUsd: '',
  unidades: ''
})

const erroresValidacion = ref({
  nombre: '',
  categoria: '',
  descripcion: '',
  ancho: '',
  alto: '',
  profundidad: '',
  material: '',
  urlFoto: '',
  precioUsd: '',
  unidades: ''
})

const errorCargaImagen = ref(false)
const formularioRegistroRef = ref(null)
const inputNombreRef = ref(null)
const btnCancelarRef = ref(null)
const btnGuardarRef = ref(null)

onMounted(async () => {
  await Promise.all([
    cargarCategorias(),
    cargarMuebles()
  ])
})

async function cargarCategorias() {
  try {
    const lista = await muebleServicio.obtenerCategorias()
    categorias.value = Array.isArray(lista) ? lista : []
  } catch (error) {
    categorias.value = []
  }
}

async function cargarMuebles() {
  cargando.value = true
  mensajeError.value = ''

  try {
    const resultado = await muebleServicio.obtenerMuebles({
      busqueda: busqueda.value
    })
    muebles.value = Array.isArray(resultado) ? resultado : []
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'Error al cargar el inventario de muebles.'
    )
    muebles.value = []
  } finally {
    cargando.value = false
  }
}

function abrirModalRegistro() {
  formulario.value = {
    nombre: '',
    categoria: categorias.value.length > 0 ? categorias.value[0] : '',
    otraCategoria: '',
    descripcion: '',
    ancho: '',
    alto: '',
    profundidad: '',
    material: '',
    urlFoto: '',
    precioUsd: '',
    unidades: 1
  }
  erroresValidacion.value = {
    nombre: '',
    categoria: '',
    descripcion: '',
    ancho: '',
    alto: '',
    profundidad: '',
    material: '',
    urlFoto: '',
    precioUsd: '',
    unidades: ''
  }
  errorCargaImagen.value = false
  modalRegistroAbierto.value = true
  nextTick(() => {
    enfocarCampo(inputNombreRef.value)
  })
}

function cerrarModalRegistro() {
  modalRegistroAbierto.value = false
}

function cerrarConEscape() {
  if (!guardando.value) {
    cerrarModalRegistro()
  }
}

function obtenerCamposNavegables() {
  if (!formularioRegistroRef.value) return []
  return Array.from(
    formularioRegistroRef.value.querySelectorAll('input, select, textarea, button')
  ).filter((el) => !el.disabled && el.getClientRects().length > 0)
}

function enfocarCampo(el) {
  if (!el) return
  el.focus()
  el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
}

function esEnBordeTexto(el, haciaAdelante) {
  if (!el) return false
  // Los inputs numéricos no exponen selectionStart en los navegadores: solo saltamos con ←/→ si están vacíos
  if (el.tagName === 'INPUT' && el.type === 'number') {
    return String(el.value ?? '') === ''
  }
  const inicio = el.selectionStart
  const fin = el.selectionEnd
  if (typeof inicio !== 'number' || typeof fin !== 'number' || inicio !== fin) {
    return false
  }
  const longitud = (el.value ?? '').length
  return haciaAdelante ? fin === longitud : inicio === 0
}

function manejarTecladoFormulario(e) {
  const teclas = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight']
  if (!teclas.includes(e.key) || e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) {
    return
  }

  const actual = e.target
  const campos = obtenerCamposNavegables()
  const indice = campos.indexOf(actual)
  if (indice === -1) return

  // Selector de categoría (<select>): ← / → cambia opción; ↑ / ↓ cambia de campo
  if (actual.tagName === 'SELECT') {
    if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
      e.preventDefault()
      const opciones = actual.options
      if (opciones.length > 0) {
        const paso = e.key === 'ArrowRight' ? 1 : -1
        const nuevoIndice = Math.max(0, Math.min(opciones.length - 1, actual.selectedIndex + paso))
        if (nuevoIndice !== actual.selectedIndex) {
          formulario.value.categoria = opciones[nuevoIndice].value
        }
      }
      return
    }
    e.preventDefault()
    const destino = e.key === 'ArrowDown' ? campos[indice + 1] : campos[indice - 1]
    enfocarCampo(destino)
    return
  }

  // Botones de acción (Cancelar / Guardar Mueble)
  if (actual.tagName === 'BUTTON') {
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      const ultimoCampo = [...campos].reverse().find((el) => el.tagName !== 'BUTTON')
      enfocarCampo(ultimoCampo)
      return
    }
    if (e.key === 'ArrowLeft') {
      e.preventDefault()
      if (actual === btnGuardarRef.value && btnCancelarRef.value) {
        enfocarCampo(btnCancelarRef.value)
      }
      return
    }
    if (e.key === 'ArrowRight') {
      e.preventDefault()
      if (actual === btnCancelarRef.value && btnGuardarRef.value) {
        enfocarCampo(btnGuardarRef.value)
      }
      return
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
    }
    return
  }

  // Área de texto (<textarea>): respeta el salto de líneas internas si el texto tiene varias líneas
  if (actual.tagName === 'TEXTAREA') {
    const inicio = actual.selectionStart
    const fin = actual.selectionEnd
    if (typeof inicio !== 'number' || inicio !== fin) return

    const valor = actual.value ?? ''
    if (e.key === 'ArrowUp') {
      const enPrimeraLinea = !valor.slice(0, inicio).includes('\n')
      if (enPrimeraLinea) {
        e.preventDefault()
        enfocarCampo(campos[indice - 1])
      }
      return
    }
    if (e.key === 'ArrowDown') {
      const enUltimaLinea = !valor.slice(fin).includes('\n')
      if (enUltimaLinea) {
        e.preventDefault()
        enfocarCampo(btnGuardarRef.value || campos[indice + 1])
      }
      return
    }
    if (e.key === 'ArrowLeft' && !e.repeat && inicio === 0) {
      e.preventDefault()
      enfocarCampo(campos[indice - 1])
      return
    }
    if (e.key === 'ArrowRight' && !e.repeat && fin === valor.length) {
      e.preventDefault()
      enfocarCampo(btnGuardarRef.value || campos[indice + 1])
    }
    return
  }

  // Inputs de texto, URL y numéricos
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    const destino = campos[indice + 1]
    enfocarCampo(destino?.tagName === 'BUTTON' && btnGuardarRef.value ? btnGuardarRef.value : destino)
    return
  }
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    enfocarCampo(campos[indice - 1])
    return
  }
  if ((e.key === 'ArrowLeft' || e.key === 'ArrowRight') && !e.repeat) {
    const haciaAdelante = e.key === 'ArrowRight'
    if (esEnBordeTexto(actual, haciaAdelante)) {
      e.preventDefault()
      const destino = campos[haciaAdelante ? indice + 1 : indice - 1]
      enfocarCampo(destino?.tagName === 'BUTTON' && btnGuardarRef.value ? btnGuardarRef.value : destino)
    }
  }
}

// Convertir precio aceptando coma o punto a número con punto decimal
function normalizarPrecio(str) {
  if (!str) return null
  const limpio = String(str).trim().replace(',', '.')
  const num = parseFloat(limpio)
  return isNaN(num) ? null : num
}

function validarFormulario() {
  let valido = true
  erroresValidacion.value = {
    nombre: '',
    categoria: '',
    descripcion: '',
    ancho: '',
    alto: '',
    profundidad: '',
    material: '',
    urlFoto: '',
    precioUsd: '',
    unidades: ''
  }

  // Nombre
  if (!formulario.value.nombre.trim()) {
    erroresValidacion.value.nombre = 'El nombre del mueble es obligatorio.'
    valido = false
  }

  // Categoría
  const catFinal = formulario.value.categoria === 'OTRA'
    ? formulario.value.otraCategoria.trim()
    : formulario.value.categoria.trim()

  if (!catFinal) {
    erroresValidacion.value.categoria = 'La categoría es obligatoria.'
    valido = false
  }

  // Descripción (obligatoria y <= 500)
  if (!formulario.value.descripcion.trim()) {
    erroresValidacion.value.descripcion = 'La descripción es obligatoria.'
    valido = false
  } else if (formulario.value.descripcion.length > 500) {
    erroresValidacion.value.descripcion = 'La descripción no puede superar los 500 caracteres.'
    valido = false
  }

  // Dimensiones en cm (ancho, alto, profundidad > 0)
  const anchoNum = parseFloat(formulario.value.ancho)
  if (isNaN(anchoNum) || anchoNum <= 0) {
    erroresValidacion.value.ancho = 'El ancho debe ser un número mayor a 0 cm.'
    valido = false
  }

  const altoNum = parseFloat(formulario.value.alto)
  if (isNaN(altoNum) || altoNum <= 0) {
    erroresValidacion.value.alto = 'El alto debe ser un número mayor a 0 cm.'
    valido = false
  }

  const profNum = parseFloat(formulario.value.profundidad)
  if (isNaN(profNum) || profNum <= 0) {
    erroresValidacion.value.profundidad = 'La profundidad debe ser un número mayor a 0 cm.'
    valido = false
  }

  // Material
  if (!formulario.value.material.trim()) {
    erroresValidacion.value.material = 'El material es obligatorio.'
    valido = false
  }

  // URL de foto
  const url = formulario.value.urlFoto.trim()
  if (!url) {
    erroresValidacion.value.urlFoto = 'La URL de la foto es obligatoria.'
    valido = false
  } else if (!/^https?:\/\/.+/i.test(url)) {
    erroresValidacion.value.urlFoto = 'Debe ser una URL válida que inicie con http:// o https://'
    valido = false
  }

  // Precio USD (mayor a 0, máximo 2 decimales, punto decimal)
  const precioNum = normalizarPrecio(formulario.value.precioUsd)
  if (precioNum === null || precioNum <= 0) {
    erroresValidacion.value.precioUsd = 'El precio debe ser un número mayor a 0.'
    valido = false
  } else {
    // Validar máximo 2 decimales
    const partes = String(formulario.value.precioUsd).replace(',', '.').split('.')
    if (partes.length === 2 && partes[1].length > 2) {
      erroresValidacion.value.precioUsd = 'El precio solo puede tener hasta 2 decimales.'
      valido = false
    }
  }

  // Unidades (0 a 100000)
  const uds = parseInt(formulario.value.unidades, 10)
  if (isNaN(uds) || uds < 0 || uds > 100000) {
    erroresValidacion.value.unidades = 'El stock debe ser un entero entre 0 y 100.000 unidades.'
    valido = false
  }

  return valido
}

async function guardarMueble() {
  if (!validarFormulario()) return

  guardando.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const catFinal = formulario.value.categoria === 'OTRA'
      ? formulario.value.otraCategoria.trim()
      : formulario.value.categoria.trim()

    const payload = {
      nombre: formulario.value.nombre.trim(),
      categoria: catFinal,
      descripcion: formulario.value.descripcion.trim(),
      dimensiones: {
        ancho: parseFloat(formulario.value.ancho),
        alto: parseFloat(formulario.value.alto),
        profundidad: parseFloat(formulario.value.profundidad)
      },
      material: formulario.value.material.trim(),
      urlFoto: formulario.value.urlFoto.trim(),
      precioUsd: normalizarPrecio(formulario.value.precioUsd),
      unidades: parseInt(formulario.value.unidades, 10)
    }

    const registrado = await muebleServicio.registrarMueble(payload)
    mensajeExito.value = `¡Mueble "${registrado.nombre}" registrado exitosamente con código ${registrado.codigo}!`

    cerrarModalRegistro()
    await Promise.all([cargarCategorias(), cargarMuebles()])
  } catch (error) {
    mensajeError.value = extraerMensajeError(
      error,
      'No se pudo registrar el mueble. Revisa los datos ingresados.'
    )
  } finally {
    guardando.value = false
  }
}

// Formateos con Intl.NumberFormat('es-VE')
function formatearUsd(valor) {
  if (valor == null || isNaN(valor)) return '$ 0,00'
  return '$ ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(valor)
}

function formatearBs(valor) {
  if (valor == null || isNaN(valor)) return 'Bs. —'
  return 'Bs. ' + new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(valor)
}
</script>

<template>
  <div class="contenedor vista-inventario">
    <!-- Encabezado de Inventario -->
    <header class="encabezado-inventario">
      <div>
        <h1 class="titulo-inventario">Inventario de Muebles</h1>
        <p class="subtitulo-inventario">
          Control de stock y registro de nuevas piezas (Administración y Ventas).
        </p>
      </div>

      <button type="button" class="btn btn-primario" @click="abrirModalRegistro">
        ＋ Registrar Mueble
      </button>
    </header>

    <!-- Alertas -->
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

    <!-- Barra de Búsqueda y Actualización -->
    <section class="barra-acciones-inventario tarjeta">
      <div class="busqueda-inventario">
        <input
          v-model="busqueda"
          type="search"
          class="campo-texto"
          placeholder="Buscar mueble en inventario..."
          @keyup.enter="cargarMuebles"
        />
        <button type="button" class="btn btn-secundario" @click="cargarMuebles">
          Buscar
        </button>
      </div>

      <div class="resumen-inventario">
        <span>Total: <strong>{{ muebles.length }}</strong> piezas</span>
        <button
          type="button"
          class="btn btn-outline btn-sm"
          :disabled="cargando"
          @click="cargarMuebles"
        >
          ↻ Actualizar
        </button>
      </div>
    </section>

    <!-- Estado de Carga -->
    <div v-if="cargando" class="cargando-inventario">
      Cargando inventario...
    </div>

    <!-- Sin Muebles -->
    <div v-else-if="muebles.length === 0" class="tarjeta estado-vacio">
      <h3>No hay muebles registrados</h3>
      <p>Comienza agregando la primera pieza con el botón "Registrar Mueble".</p>
      <button type="button" class="btn btn-primario" @click="abrirModalRegistro">
        ＋ Registrar Mueble
      </button>
    </div>

    <!-- Tabla de Inventario Responsive -->
    <div v-else class="tarjeta contenedor-tabla-inventario">
      <!-- Versión Escritorio: Tabla completa -->
      <table class="tabla-inventario">
        <thead>
          <tr>
            <th>Código</th>
            <th>Foto</th>
            <th>Nombre y Categoría</th>
            <th>Dimensiones (cm)</th>
            <th>Material</th>
            <th>Precio USD</th>
            <th>Precio Bs.</th>
            <th>Stock</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in muebles" :key="m.id">
            <td>
              <span class="codigo-tabla">{{ m.codigo }}</span>
            </td>
            <td>
              <img
                v-if="m.urlFoto"
                :src="m.urlFoto"
                :alt="m.nombre"
                class="miniatura-tabla"
                loading="lazy"
                @error="$event.target.style.display = 'none'"
              />
              <span v-else class="sin-miniatura">—</span>
            </td>
            <td>
              <strong>{{ m.nombre }}</strong>
              <div class="categoria-subtexto">{{ m.categoria }}</div>
            </td>
            <td class="texto-dimensiones">
              <template v-if="m.dimensiones">
                {{ m.dimensiones.ancho }} × {{ m.dimensiones.profundidad }} × {{ m.dimensiones.alto }}
              </template>
              <template v-else>—</template>
            </td>
            <td>{{ m.material }}</td>
            <td class="precio-col">
              <strong>{{ formatearUsd(m.precioUsd) }}</strong>
            </td>
            <td class="precio-col">
              {{ formatearBs(m.precioBs) }}
            </td>
            <td>
              <span
                :class="[
                  'insignia-stock-tabla',
                  m.unidades > 0 ? 'stock-ok' : 'stock-cero'
                ]"
              >
                {{ m.unidades ?? 0 }} uds.
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Versión Móvil: Lista de tarjetas para usabilidad mobile-first -->
      <div class="lista-movil-inventario">
        <article
          v-for="m in muebles"
          :key="m.id"
          class="tarjeta-item-movil"
        >
          <div class="cabecera-item-movil">
            <div>
              <span class="codigo-tabla">{{ m.codigo }}</span>
              <h3 class="nombre-item-movil">{{ m.nombre }}</h3>
              <span class="categoria-subtexto">{{ m.categoria }}</span>
            </div>
            <span
              :class="[
                'insignia-stock-tabla',
                m.unidades > 0 ? 'stock-ok' : 'stock-cero'
              ]"
            >
              {{ m.unidades ?? 0 }} uds.
            </span>
          </div>

          <div class="detalles-item-movil">
            <p><strong>Material:</strong> {{ m.material }}</p>
            <p v-if="m.dimensiones">
              <strong>Dimensiones:</strong>
              {{ m.dimensiones.ancho }} × {{ m.dimensiones.profundidad }} × {{ m.dimensiones.alto }} cm
            </p>
            <div class="precios-item-movil">
              <span class="precio-usd-movil">{{ formatearUsd(m.precioUsd) }}</span>
              <span class="precio-bs-movil">{{ formatearBs(m.precioBs) }}</span>
            </div>
          </div>
        </article>
      </div>

      <div class="pie-tabla-info">
        <small class="texto-ayuda">
          * Nota: Edición y eliminación de muebles reservadas para Sprint 2 según backlog oficial.
        </small>
      </div>
    </div>

    <!-- MODAL: Registrar Mueble -->
    <div
      v-if="modalRegistroAbierto"
      class="superposicion-modal"
      role="dialog"
      aria-labelledby="titulo-modal"
      aria-modal="true"
      @keydown.esc="cerrarConEscape"
    >
      <div class="contenido-modal tarjeta">
        <div class="cabecera-modal">
          <h2 id="titulo-modal">Registrar Nuevo Mueble</h2>
          <button
            type="button"
            class="btn-cerrar-modal"
            aria-label="Cerrar modal"
            @click="cerrarModalRegistro"
          >
            &times;
          </button>
        </div>

        <p class="ayuda-teclado-modal">
          ⌨️ Navega entre los campos con las flechas <strong>↑ ↓</strong> (usa <strong>← →</strong> en Categoría, botones o bordes) y presiona <strong>Esc</strong> para cerrar.
        </p>

        <form
          ref="formularioRegistroRef"
          class="formulario"
          @submit.prevent="guardarMueble"
          @keydown="manejarTecladoFormulario"
          novalidate
        >
          <!-- Nombre -->
          <div class="grupo-campo">
            <label for="reg-nombre" class="etiqueta">
              Nombre de la pieza <span class="requerido">*</span>
            </label>
            <input
              id="reg-nombre"
              ref="inputNombreRef"
              v-model="formulario.nombre"
              type="text"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.nombre }"
              placeholder="Ej: Sofá Alba"
              required
            />
            <span v-if="erroresValidacion.nombre" class="texto-error">
              {{ erroresValidacion.nombre }}
            </span>
          </div>

          <!-- Categoría -->
          <div class="fila-doble">
            <div class="grupo-campo">
              <label for="reg-categoria" class="etiqueta">
                Categoría <span class="requerido">*</span>
              </label>
              <select
                id="reg-categoria"
                v-model="formulario.categoria"
                class="campo-selector"
                :class="{ 'con-error': erroresValidacion.categoria }"
              >
                <option v-for="c in categorias" :key="c" :value="c">{{ c }}</option>
                <option value="OTRA">＋ Nueva categoría...</option>
              </select>
            </div>

            <div v-if="formulario.categoria === 'OTRA'" class="grupo-campo">
              <label for="reg-otra-categoria" class="etiqueta">
                Escribe la categoría <span class="requerido">*</span>
              </label>
              <input
                id="reg-otra-categoria"
                v-model="formulario.otraCategoria"
                type="text"
                class="campo-texto"
                :class="{ 'con-error': erroresValidacion.categoria }"
                placeholder="Ej: Terraza"
              />
            </div>
          </div>
          <span v-if="erroresValidacion.categoria" class="texto-error">
            {{ erroresValidacion.categoria }}
          </span>

          <!-- Material -->
          <div class="grupo-campo">
            <label for="reg-material" class="etiqueta">
              Material principal <span class="requerido">*</span>
            </label>
            <input
              id="reg-material"
              v-model="formulario.material"
              type="text"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.material }"
              placeholder="Ej: Madera de nogal y lino beige"
              required
            />
            <span v-if="erroresValidacion.material" class="texto-error">
              {{ erroresValidacion.material }}
            </span>
          </div>

          <!-- Dimensiones (Ancho, Alto, Profundidad en cm) -->
          <div class="grupo-campo">
            <label class="etiqueta">
              Dimensiones (en centímetros) <span class="requerido">*</span>
            </label>
            <div class="fila-dimensiones">
              <div>
                <input
                  v-model="formulario.ancho"
                  type="number"
                  step="any"
                  class="campo-texto"
                  :class="{ 'con-error': erroresValidacion.ancho }"
                  placeholder="Ancho (cm)"
                  required
                />
                <span v-if="erroresValidacion.ancho" class="texto-error">
                  {{ erroresValidacion.ancho }}
                </span>
              </div>
              <div>
                <input
                  v-model="formulario.profundidad"
                  type="number"
                  step="any"
                  class="campo-texto"
                  :class="{ 'con-error': erroresValidacion.profundidad }"
                  placeholder="Profundidad (cm)"
                  required
                />
                <span v-if="erroresValidacion.profundidad" class="texto-error">
                  {{ erroresValidacion.profundidad }}
                </span>
              </div>
              <div>
                <input
                  v-model="formulario.alto"
                  type="number"
                  step="any"
                  class="campo-texto"
                  :class="{ 'con-error': erroresValidacion.alto }"
                  placeholder="Alto (cm)"
                  required
                />
                <span v-if="erroresValidacion.alto" class="texto-error">
                  {{ erroresValidacion.alto }}
                </span>
              </div>
            </div>
            <span class="texto-ayuda">Se guardan como Ancho × Profundidad × Alto.</span>
          </div>

          <!-- Precios y Stock -->
          <div class="fila-doble">
            <div class="grupo-campo">
              <label for="reg-precio" class="etiqueta">
                Precio base USD ($) <span class="requerido">*</span>
              </label>
              <input
                id="reg-precio"
                v-model="formulario.precioUsd"
                type="text"
                class="campo-texto"
                :class="{ 'con-error': erroresValidacion.precioUsd }"
                placeholder="Ej: 480.00"
                required
              />
              <span v-if="erroresValidacion.precioUsd" class="texto-error">
                {{ erroresValidacion.precioUsd }}
              </span>
              <span class="texto-ayuda">Acepta punto o coma. Máximo 2 decimales.</span>
            </div>

            <div class="grupo-campo">
              <label for="reg-unidades" class="etiqueta">
                Unidades en Stock <span class="requerido">*</span>
              </label>
              <input
                id="reg-unidades"
                v-model.number="formulario.unidades"
                type="number"
                min="0"
                max="100000"
                class="campo-texto"
                :class="{ 'con-error': erroresValidacion.unidades }"
                placeholder="Ej: 8"
                required
              />
              <span v-if="erroresValidacion.unidades" class="texto-error">
                {{ erroresValidacion.unidades }}
              </span>
              <span class="texto-ayuda">Stock inicial entre 0 y 100.000.</span>
            </div>
          </div>

          <!-- URL de Foto y Vista Previa Inmediata (Restricción 4) -->
          <div class="grupo-campo">
            <label for="reg-foto" class="etiqueta">
              URL de la foto (http/https) <span class="requerido">*</span>
            </label>
            <input
              id="reg-foto"
              v-model="formulario.urlFoto"
              type="url"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.urlFoto }"
              placeholder="https://ejemplo.com/fotos/sofa.jpg"
              required
              @input="errorCargaImagen = false"
            />
            <span v-if="erroresValidacion.urlFoto" class="texto-error">
              {{ erroresValidacion.urlFoto }}
            </span>

            <!-- Vista previa en tiempo real -->
            <div v-if="formulario.urlFoto" class="vista-previa-contenedor">
              <span class="etiqueta-vista-previa">Vista previa de la imagen:</span>
              <img
                v-show="!errorCargaImagen"
                :src="formulario.urlFoto"
                alt="Vista previa"
                class="imagen-previa"
                @error="errorCargaImagen = true"
              />
              <div v-if="errorCargaImagen" class="alerta-error-imagen">
                ⚠️ No se pudo cargar la imagen desde la URL provista. Verifica que sea un enlace directo.
              </div>
            </div>
          </div>

          <!-- Descripción -->
          <div class="grupo-campo">
            <label for="reg-descripcion" class="etiqueta">
              Descripción detallada <span class="requerido">*</span>
            </label>
            <textarea
              id="reg-descripcion"
              v-model="formulario.descripcion"
              rows="3"
              maxlength="500"
              class="campo-area"
              :class="{ 'con-error': erroresValidacion.descripcion }"
              placeholder="Descripción del acabado, confort y uso sugerido..."
              required
            ></textarea>
            <div class="pie-descripcion">
              <span v-if="erroresValidacion.descripcion" class="texto-error">
                {{ erroresValidacion.descripcion }}
              </span>
              <span class="texto-ayuda">{{ formulario.descripcion.length }}/500 caracteres</span>
            </div>
          </div>

          <!-- Botones de Acción -->
          <div class="acciones-modal">
            <button
              ref="btnCancelarRef"
              type="button"
              class="btn btn-outline"
              :disabled="guardando"
              @click="cerrarModalRegistro"
            >
              Cancelar
            </button>
            <button
              ref="btnGuardarRef"
              type="submit"
              class="btn btn-primario"
              :disabled="guardando"
            >
              <span v-if="guardando">Guardando en inventario...</span>
              <span v-else>Guardar Mueble</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vista-inventario {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.encabezado-inventario {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.titulo-inventario {
  font-size: 1.85rem;
  color: var(--color-nogal-primario);
  margin-bottom: 0.25rem;
}

.subtitulo-inventario {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
  margin-bottom: 0;
}

.barra-acciones-inventario {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.busqueda-inventario {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 250px;
}

.resumen-inventario {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
  color: var(--color-texto-secundario);
}

.cargando-inventario {
  text-align: center;
  padding: 3rem;
  color: var(--color-texto-secundario);
}

.contenedor-tabla-inventario {
  padding: 0;
  overflow: hidden;
}

.tabla-inventario {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.tabla-inventario th,
.tabla-inventario td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-borde);
  font-size: 0.875rem;
}

.tabla-inventario th {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  font-weight: 600;
  white-space: nowrap;
}

.codigo-tabla {
  font-family: monospace;
  font-weight: 700;
  color: var(--color-nogal-secundario);
  background-color: var(--color-nogal-fondo);
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.miniatura-tabla {
  width: 50px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid var(--color-borde);
}

.categoria-subtexto {
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
  text-transform: uppercase;
}

.texto-dimensiones {
  font-size: 0.8rem;
  white-space: nowrap;
}

.precio-col {
  white-space: nowrap;
}

.insignia-stock-tabla {
  display: inline-block;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}

.stock-ok {
  background-color: var(--color-exito-fondo);
  color: var(--color-exito);
}

.stock-cero {
  background-color: var(--color-error-fondo);
  color: var(--color-error);
}

.pie-tabla-info {
  padding: 0.75rem 1rem;
  background-color: var(--color-fondo);
  border-top: 1px solid var(--color-borde);
}

/* Versión Móvil de Inventario */
.lista-movil-inventario {
  display: none;
  padding: 1rem;
  flex-direction: column;
  gap: 1rem;
}

.tarjeta-item-movil {
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  padding: 1rem;
  background-color: var(--color-superficie);
}

.cabecera-item-movil {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.nombre-item-movil {
  font-size: 1.1rem;
  margin-top: 0.25rem;
  margin-bottom: 0.1rem;
}

.detalles-item-movil p {
  margin-bottom: 0.25rem;
  font-size: 0.85rem;
}

.precios-item-movil {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
  font-weight: 700;
}

.precio-usd-movil {
  color: var(--color-nogal-primario);
}

.precio-bs-movil {
  color: var(--color-texto-secundario);
}

/* Modal */
.superposicion-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 1rem;
  overflow-y: auto;
}

.contenido-modal {
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 1.5rem;
}

.cabecera-modal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-borde);
  padding-bottom: 0.75rem;
  margin-bottom: 1.25rem;
}

.btn-cerrar-modal {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  line-height: 1;
  color: var(--color-texto-secundario);
}

.fila-doble {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.fila-dimensiones {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}

.vista-previa-contenedor {
  margin-top: 0.5rem;
  padding: 0.5rem;
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  background-color: var(--color-nogal-fondo);
}

.etiqueta-vista-previa {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-nogal-primario);
  margin-bottom: 0.35rem;
}

.imagen-previa {
  max-width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.alerta-error-imagen {
  font-size: 0.8rem;
  color: var(--color-error);
  padding: 0.4rem;
}

.pie-descripcion {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ayuda-teclado-modal {
  font-size: 0.8rem;
  color: var(--color-nogal-primario);
  background-color: var(--color-nogal-fondo);
  border: 1px solid var(--color-nogal-claro);
  border-radius: var(--radio-control);
  padding: 0.55rem 0.75rem;
  margin-bottom: 1rem;
}

.acciones-modal {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-borde);
}

.acciones-modal .btn:focus-visible {
  outline: 3px solid rgba(93, 64, 55, 0.35);
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .tabla-inventario {
    display: none;
  }

  .lista-movil-inventario {
    display: flex;
  }

  .fila-doble,
  .fila-dimensiones {
    grid-template-columns: 1fr;
  }
}
</style>
