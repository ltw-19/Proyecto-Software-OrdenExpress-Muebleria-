<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  mostrar: {
    type: Boolean,
    default: false
  },
  titulo: {
    type: String,
    default: 'Atención'
  },
  mensaje: {
    type: String,
    default: ''
  },
  tipo: {
    type: String,
    default: 'error',
    validator: (v) => ['error', 'aviso', 'exito', 'info'].includes(v)
  },
  textoBoton: {
    type: String,
    default: 'Entendido'
  },
  cerrarAlFondo: {
    type: Boolean,
    default: true
  },
  cerrarConEscape: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['cerrar', 'update:mostrar'])

const idTitulo = `titulo-flotante-${Math.random().toString(36).substring(2, 9)}`
const cajaFlotanteRef = ref(null)
const pieFlotanteRef = ref(null)
const botonAccionRef = ref(null)
let elementoPrevio = null

function enfocarDialogo() {
  nextTick(() => {
    if (botonAccionRef.value) {
      botonAccionRef.value.focus()
      return
    }
    const primerBoton = pieFlotanteRef.value?.querySelector('button:not(:disabled)')
    if (primerBoton) {
      primerBoton.focus()
      return
    }
    cajaFlotanteRef.value?.focus()
  })
}

function cerrar() {
  emit('cerrar')
  emit('update:mostrar', false)
}

function manejarClicFondo(evento) {
  if (props.cerrarAlFondo && evento.target === evento.currentTarget) {
    cerrar()
  }
}

function manejarTeclado(evento) {
  if (props.mostrar && props.cerrarConEscape && evento.key === 'Escape') {
    cerrar()
  }
}

watch(
  () => props.mostrar,
  (nuevoValor) => {
    if (nuevoValor) {
      elementoPrevio = document.activeElement
      document.body.style.overflow = 'hidden'
      enfocarDialogo()
    } else {
      document.body.style.overflow = ''
      if (elementoPrevio && elementoPrevio.isConnected && typeof elementoPrevio.focus === 'function') {
        elementoPrevio.focus()
      }
      elementoPrevio = null
    }
  }
)

onMounted(() => {
  window.addEventListener('keydown', manejarTeclado)
  if (props.mostrar) {
    elementoPrevio = document.activeElement
    document.body.style.overflow = 'hidden'
    enfocarDialogo()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', manejarTeclado)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="flotante-anim">
      <div
        v-if="mostrar"
        class="superposicion-flotante"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="idTitulo"
        @click="manejarClicFondo"
      >
        <div ref="cajaFlotanteRef" class="caja-flotante" :class="`tipo-${tipo}`" role="document" tabindex="-1">
          <!-- Botón de cierre en la esquina superior derecha -->
          <button
            type="button"
            class="btn-cerrar-flotante"
            aria-label="Cerrar ventana"
            @click="cerrar"
          >
            &times;
          </button>

          <!-- Ícono temático -->
          <div class="icono-contenedor-flotante">
            <div :class="['insignia-icono', `insignia-${tipo}`]">
              <!-- Ícono de Error / Stock -->
              <svg
                v-if="tipo === 'error'"
                class="icono-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>

              <!-- Ícono de Aviso -->
              <svg
                v-else-if="tipo === 'aviso'"
                class="icono-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>

              <!-- Ícono de Éxito -->
              <svg
                v-else-if="tipo === 'exito'"
                class="icono-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>

              <!-- Ícono Informativo -->
              <svg
                v-else
                class="icono-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
            </div>
          </div>

          <!-- Cabecera y Mensaje -->
          <div class="cuerpo-flotante">
            <h3 :id="idTitulo" class="titulo-flotante">
              {{ titulo }}
            </h3>
            <p class="mensaje-flotante">
              <slot>{{ mensaje }}</slot>
            </p>
          </div>

          <!-- Botón de acción -->
          <div ref="pieFlotanteRef" class="pie-flotante">
            <slot name="acciones">
              <button
                ref="botonAccionRef"
                type="button"
                class="btn btn-primario btn-accion-flotante"
                @click="cerrar"
              >
                {{ textoBoton }}
              </button>
            </slot>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Superposición con fondo semitransparente y desenfoque */
.superposicion-flotante {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(33, 37, 41, 0.55);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.25rem;
  overflow-y: auto;
}

/* Caja de la ventana flotante */
.caja-flotante {
  position: relative;
  background-color: var(--color-superficie);
  border-radius: var(--radio-tarjeta);
  border: 1px solid var(--color-borde);
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.22), 0 6px 16px rgba(0, 0, 0, 0.08);
  width: 100%;
  max-width: 440px;
  padding: 2rem 1.75rem 1.5rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.caja-flotante:focus {
  outline: none;
}

/* Botón cerrar '×' */
.btn-cerrar-flotante {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-texto-mutado);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radio-control);
  transition: color 0.15s ease, background-color 0.15s ease;
}

.btn-cerrar-flotante:hover {
  color: var(--color-texto-principal);
  background-color: var(--color-nogal-fondo);
}

.btn-cerrar-flotante:focus-visible {
  outline: 2px solid var(--color-nogal-primario);
  outline-offset: 2px;
}

/* Contenedor del ícono */
.icono-contenedor-flotante {
  margin-bottom: 1.1rem;
}

.insignia-icono {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.insignia-error {
  background-color: var(--color-error-fondo);
  color: var(--color-error);
  border: 1px solid #FFCDD2;
}

.insignia-aviso {
  background-color: var(--color-aviso-fondo);
  color: var(--color-aviso);
  border: 1px solid #FFE0B2;
}

.insignia-exito {
  background-color: var(--color-exito-fondo);
  color: var(--color-exito);
  border: 1px solid #C8E6C9;
}

.insignia-info {
  background-color: #E3F2FD;
  color: #1565C0;
  border: 1px solid #BBDEFB;
}

.icono-svg {
  width: 30px;
  height: 30px;
}

/* Cuerpo con título y descripción */
.cuerpo-flotante {
  margin-bottom: 1.5rem;
  width: 100%;
}

.titulo-flotante {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-nogal-primario);
  margin-bottom: 0.6rem;
  line-height: 1.3;
}

.mensaje-flotante {
  font-size: 0.95rem;
  color: var(--color-texto-secundario);
  line-height: 1.55;
  margin: 0;
  word-break: break-word;
}

/* Pie con botón de confirmación */
.pie-flotante {
  width: 100%;
}

.btn-accion-flotante {
  width: 100%;
  padding: 0.75rem 1.25rem;
  font-size: 0.95rem;
  border-radius: var(--radio-control);
}

.btn-accion-flotante:focus-visible {
  outline: 3px solid rgba(141, 110, 99, 0.45);
  outline-offset: 2px;
}

/* Transiciones animadas de entrada y salida */
.flotante-anim-enter-active,
.flotante-anim-leave-active {
  transition: opacity 0.22s ease;
}

.flotante-anim-enter-from,
.flotante-anim-leave-to {
  opacity: 0;
}

.flotante-anim-enter-active .caja-flotante {
  transition: transform 0.24s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.22s ease;
}

.flotante-anim-leave-active .caja-flotante {
  transition: transform 0.16s ease-in, opacity 0.16s ease-in;
}

.flotante-anim-enter-from .caja-flotante {
  opacity: 0;
  transform: scale(0.92) translateY(10px);
}

.flotante-anim-leave-to .caja-flotante {
  opacity: 0;
  transform: scale(0.96) translateY(-4px);
}

@media (max-width: 480px) {
  .caja-flotante {
    padding: 1.5rem 1.25rem 1.25rem;
  }

  .insignia-icono {
    width: 48px;
    height: 48px;
  }

  .icono-svg {
    width: 26px;
    height: 26px;
  }

  .titulo-flotante {
    font-size: 1.15rem;
  }

  .mensaje-flotante {
    font-size: 0.9rem;
  }
}
</style>
