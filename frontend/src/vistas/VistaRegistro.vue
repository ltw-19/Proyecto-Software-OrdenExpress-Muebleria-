<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { cuentaServicio, extraerMensajeError } from '../servicios/cuentaServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()

const formulario = ref({
  nombre: '',
  apellido: '',
  correo: '',
  telefono: '',
  clave: '',
  confirmarClave: '',
  direccion: ''
})

const cargando = ref(false)
const mensajeError = ref('')

const erroresValidacion = ref({
  nombre: '',
  apellido: '',
  correo: '',
  telefono: '',
  clave: '',
  confirmarClave: '',
  direccion: ''
})

function limpiarTelefono(tel) {
  // Quita espacios, guiones y paréntesis según restricción de negocio
  return tel.replace(/[\s\-\(\)]/g, '')
}

function validarCampos() {
  let valido = true
  erroresValidacion.value = {
    nombre: '',
    apellido: '',
    correo: '',
    telefono: '',
    clave: '',
    confirmarClave: '',
    direccion: ''
  }
  mensajeError.value = ''

  if (!formulario.value.nombre.trim()) {
    erroresValidacion.value.nombre = 'El nombre es obligatorio.'
    valido = false
  }

  if (!formulario.value.apellido.trim()) {
    erroresValidacion.value.apellido = 'El apellido es obligatorio.'
    valido = false
  }

  const correoLimpio = formulario.value.correo.trim()
  if (!correoLimpio) {
    erroresValidacion.value.correo = 'El correo electrónico es obligatorio.'
    valido = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correoLimpio)) {
    erroresValidacion.value.correo = 'Ingresa un formato de correo válido.'
    valido = false
  }

  const telLimpio = limpiarTelefono(formulario.value.telefono)
  if (!telLimpio) {
    erroresValidacion.value.telefono = 'El teléfono es obligatorio.'
    valido = false
  } else if (!/^\+?[0-9]{10,15}$/.test(telLimpio)) {
    erroresValidacion.value.telefono = 'El teléfono debe tener entre 10 y 15 dígitos (ej: +584121234567 o 04121234567).'
    valido = false
  }

  if (!formulario.value.clave) {
    erroresValidacion.value.clave = 'La contraseña es obligatoria.'
    valido = false
  } else if (formulario.value.clave.length < 8 || formulario.value.clave.length > 64) {
    erroresValidacion.value.clave = 'La contraseña debe tener entre 8 y 64 caracteres.'
    valido = false
  }

  if (!formulario.value.confirmarClave) {
    erroresValidacion.value.confirmarClave = 'Confirma tu contraseña.'
    valido = false
  } else if (formulario.value.clave !== formulario.value.confirmarClave) {
    erroresValidacion.value.confirmarClave = 'Las contraseñas no coinciden.'
    valido = false
  }

  if (formulario.value.direccion && formulario.value.direccion.length > 200) {
    erroresValidacion.value.direccion = 'La dirección no puede exceder los 200 caracteres.'
    valido = false
  }

  return valido
}

async function manejarRegistro() {
  if (!validarCampos()) return

  cargando.value = true
  mensajeError.value = ''

  try {
    const telLimpio = limpiarTelefono(formulario.value.telefono)
    const payload = {
      nombre: formulario.value.nombre.trim(),
      apellido: formulario.value.apellido.trim(),
      correo: formulario.value.correo.trim(),
      clave: formulario.value.clave,
      telefono: telLimpio
    }

    if (formulario.value.direccion.trim()) {
      payload.direccion = formulario.value.direccion.trim()
    }

    await cuentaServicio.registrar(payload)

    // Tras el 201 lleva al ingreso (no inicia sesión sola, regla de negocio)
    router.push({ path: '/ingreso', query: { registrado: 'true' } })
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al registrar la cuenta.')
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="contenedor-autenticacion">
    <div class="tarjeta tarjeta-registro">
      <div class="encabezado-autenticacion">
        <h1 class="titulo-autenticacion">Crear Cuenta</h1>
        <p class="subtitulo-autenticacion">
          Regístrate como cliente para armar tu pre-orden de muebles.
        </p>
      </div>

      <AlertaMensaje
        v-if="mensajeError"
        tipo="error"
        :mensaje="mensajeError"
        @cerrar="mensajeError = ''"
      />

      <form class="formulario" @submit.prevent="manejarRegistro" novalidate>
        <div class="fila-doble">
          <div class="grupo-campo">
            <label for="nombre" class="etiqueta">
              Nombre <span class="requerido">*</span>
            </label>
            <input
              id="nombre"
              v-model="formulario.nombre"
              type="text"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.nombre }"
              placeholder="Ej: Carlos"
              required
            />
            <span v-if="erroresValidacion.nombre" class="texto-error">
              {{ erroresValidacion.nombre }}
            </span>
          </div>

          <div class="grupo-campo">
            <label for="apellido" class="etiqueta">
              Apellido <span class="requerido">*</span>
            </label>
            <input
              id="apellido"
              v-model="formulario.apellido"
              type="text"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.apellido }"
              placeholder="Ej: Pérez"
              required
            />
            <span v-if="erroresValidacion.apellido" class="texto-error">
              {{ erroresValidacion.apellido }}
            </span>
          </div>
        </div>

        <div class="fila-doble">
          <div class="grupo-campo">
            <label for="correo" class="etiqueta">
              Correo Electrónico <span class="requerido">*</span>
            </label>
            <input
              id="correo"
              v-model="formulario.correo"
              type="email"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.correo }"
              placeholder="carlos@correo.com"
              required
            />
            <span v-if="erroresValidacion.correo" class="texto-error">
              {{ erroresValidacion.correo }}
            </span>
          </div>

          <div class="grupo-campo">
            <label for="telefono" class="etiqueta">
              Teléfono <span class="requerido">*</span>
            </label>
            <input
              id="telefono"
              v-model="formulario.telefono"
              type="tel"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.telefono }"
              placeholder="04121234567"
              required
            />
            <span v-if="erroresValidacion.telefono" class="texto-error">
              {{ erroresValidacion.telefono }}
            </span>
            <span class="texto-ayuda">10 a 15 dígitos. Quita espacios y guiones automáticamente.</span>
          </div>
        </div>

        <div class="fila-doble">
          <div class="grupo-campo">
            <label for="clave" class="etiqueta">
              Contraseña <span class="requerido">*</span>
            </label>
            <input
              id="clave"
              v-model="formulario.clave"
              type="password"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.clave }"
              placeholder="Mínimo 8 caracteres"
              required
            />
            <span v-if="erroresValidacion.clave" class="texto-error">
              {{ erroresValidacion.clave }}
            </span>
          </div>

          <div class="grupo-campo">
            <label for="confirmarClave" class="etiqueta">
              Confirmar Contraseña <span class="requerido">*</span>
            </label>
            <input
              id="confirmarClave"
              v-model="formulario.confirmarClave"
              type="password"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.confirmarClave }"
              placeholder="Repite tu contraseña"
              required
            />
            <span v-if="erroresValidacion.confirmarClave" class="texto-error">
              {{ erroresValidacion.confirmarClave }}
            </span>
          </div>
        </div>

        <div class="grupo-campo">
          <label for="direccion" class="etiqueta">
            Dirección de Entrega (Opcional)
          </label>
          <textarea
            id="direccion"
            v-model="formulario.direccion"
            rows="2"
            class="campo-area"
            :class="{ 'con-error': erroresValidacion.direccion }"
            placeholder="Calle, sector, punto de referencia (máx. 200 caracteres)"
            maxlength="200"
          ></textarea>
          <span v-if="erroresValidacion.direccion" class="texto-error">
            {{ erroresValidacion.direccion }}
          </span>
          <span class="texto-ayuda">{{ formulario.direccion.length }}/200 caracteres</span>
        </div>

        <button
          type="submit"
          class="btn btn-primario btn-bloque"
          :disabled="cargando"
        >
          <span v-if="cargando">Creando cuenta...</span>
          <span v-else>Crear Cuenta</span>
        </button>
      </form>

      <div class="pie-autenticacion">
        <p>
          ¿Ya tienes cuenta?
          <router-link to="/ingreso">Inicia sesión</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contenedor-autenticacion {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem 1rem;
}

.tarjeta-registro {
  width: 100%;
  max-width: 620px;
}

.encabezado-autenticacion {
  text-align: center;
  margin-bottom: 1.5rem;
}

.titulo-autenticacion {
  font-size: 1.6rem;
  color: var(--color-nogal-primario);
}

.subtitulo-autenticacion {
  font-size: 0.9rem;
  color: var(--color-texto-secundario);
}

.fila-doble {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.btn-bloque {
  width: 100%;
  padding: 0.8rem;
  margin-top: 0.5rem;
}

.pie-autenticacion {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-borde);
  text-align: center;
  font-size: 0.9rem;
}

@media (max-width: 600px) {
  .fila-doble {
    grid-template-columns: 1fr;
  }
}
</style>
