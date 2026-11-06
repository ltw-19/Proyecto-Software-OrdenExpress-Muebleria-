<script setup>
import { ref, onMounted } from 'vue'
import { sesion } from '../estado/sesion.js'
import { cuentaServicio, extraerMensajeError } from '../servicios/cuentaServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const formulario = ref({
  nombre: '',
  apellido: '',
  correo: '',
  telefono: '',
  direccion: ''
})

const cargando = ref(false)
const mensajeError = ref('')
const mensajeExito = ref('')

const erroresValidacion = ref({
  nombre: '',
  apellido: '',
  correo: '',
  telefono: '',
  direccion: ''
})

onMounted(() => {
  // Precarga con los datos de la sesión del usuario autenticado
  if (sesion.usuario.value) {
    formulario.value = {
      nombre: sesion.usuario.value.nombre || '',
      apellido: sesion.usuario.value.apellido || '',
      correo: sesion.usuario.value.correo || '',
      telefono: sesion.usuario.value.telefono || '',
      direccion: sesion.usuario.value.direccion || ''
    }
  }
})

function limpiarTelefono(tel) {
  return tel.replace(/[\s\-\(\)]/g, '')
}

function validarCampos() {
  let valido = true
  erroresValidacion.value = {
    nombre: '',
    apellido: '',
    correo: '',
    telefono: '',
    direccion: ''
  }
  mensajeError.value = ''
  mensajeExito.value = ''

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
    erroresValidacion.value.telefono = 'El teléfono debe tener entre 10 y 15 dígitos.'
    valido = false
  }

  if (formulario.value.direccion && formulario.value.direccion.length > 200) {
    erroresValidacion.value.direccion = 'La dirección no puede exceder los 200 caracteres.'
    valido = false
  }

  return valido
}

async function guardarCambios() {
  if (!validarCampos()) return

  cargando.value = true
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const telLimpio = limpiarTelefono(formulario.value.telefono)
    const payload = {
      nombre: formulario.value.nombre.trim(),
      apellido: formulario.value.apellido.trim(),
      correo: formulario.value.correo.trim(),
      telefono: telLimpio,
      direccion: formulario.value.direccion.trim()
    }

    const respuesta = await cuentaServicio.modificarMisDatos(payload)

    // Actualizar estado de sesión en memoria y localStorage
    sesion.actualizarDatos(respuesta)

    mensajeExito.value = 'Tus datos personales se han actualizado correctamente.'
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al actualizar los datos personales.')
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="contenedor">
    <div class="tarjeta tarjeta-perfil">
      <div class="encabezado-perfil">
        <div class="titulo-con-rol">
          <h1>Mis Datos Personales</h1>
          <span :class="['insignia', `insignia-${sesion.rolActual.value?.toLowerCase()}`]">
            {{ sesion.rolActual.value }}
          </span>
        </div>
        <p class="subtitulo">
          Consulta y actualiza tu información de contacto y entrega.
        </p>
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

      <form class="formulario" @submit.prevent="guardarCambios" novalidate>
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
              Correo Electrónico (Editable) <span class="requerido">*</span>
            </label>
            <input
              id="correo"
              v-model="formulario.correo"
              type="email"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.correo }"
              required
            />
            <span v-if="erroresValidacion.correo" class="texto-error">
              {{ erroresValidacion.correo }}
            </span>
          </div>

          <div class="grupo-campo">
            <label for="telefono" class="etiqueta">
              Teléfono de Contacto <span class="requerido">*</span>
            </label>
            <input
              id="telefono"
              v-model="formulario.telefono"
              type="tel"
              class="campo-texto"
              :class="{ 'con-error': erroresValidacion.telefono }"
              required
            />
            <span v-if="erroresValidacion.telefono" class="texto-error">
              {{ erroresValidacion.telefono }}
            </span>
          </div>
        </div>

        <div class="grupo-campo">
          <label for="direccion" class="etiqueta">
            Dirección Habitual de Entrega
          </label>
          <textarea
            id="direccion"
            v-model="formulario.direccion"
            rows="3"
            class="campo-area"
            :class="{ 'con-error': erroresValidacion.direccion }"
            maxlength="200"
            placeholder="Dirección para precargar en tus pedidos"
          ></textarea>
          <span v-if="erroresValidacion.direccion" class="texto-error">
            {{ erroresValidacion.direccion }}
          </span>
          <span class="texto-ayuda">{{ formulario.direccion.length }}/200 caracteres</span>
        </div>

        <div class="acciones-formulario">
          <button type="submit" class="btn btn-primario" :disabled="cargando">
            <span v-if="cargando">Guardando cambios...</span>
            <span v-else>Guardar Cambios</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.tarjeta-perfil {
  max-width: 720px;
  margin: 1.5rem auto;
}

.encabezado-perfil {
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--color-borde);
  padding-bottom: 1rem;
}

.titulo-con-rol {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.fila-doble {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.acciones-formulario {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 640px) {
  .fila-doble {
    grid-template-columns: 1fr;
  }
}
</style>
