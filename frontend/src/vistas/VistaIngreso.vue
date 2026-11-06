<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { cuentaServicio, extraerMensajeError } from '../servicios/cuentaServicio.js'
import { sesion } from '../estado/sesion.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()
const route = useRoute()

const correo = ref('')
const clave = ref('')
const cargando = ref(false)
const mensajeError = ref('')
const mensajeExito = ref('')

const erroresValidacion = ref({
  correo: '',
  clave: ''
})

onMounted(() => {
  // Si viene con mensaje de redirección tras registro exitoso
  if (route.query.registrado === 'true') {
    mensajeExito.value = '¡Tu cuenta ha sido creada con éxito! Ya puedes iniciar sesión.'
  }
})

function validarCampos() {
  let valido = true
  erroresValidacion.value = { correo: '', clave: '' }
  mensajeError.value = ''

  const correoLimpio = correo.value.trim()
  if (!correoLimpio) {
    erroresValidacion.value.correo = 'El correo electrónico es obligatorio.'
    valido = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correoLimpio)) {
    erroresValidacion.value.correo = 'Ingresa un formato de correo válido.'
    valido = false
  }

  if (!clave.value) {
    erroresValidacion.value.clave = 'La contraseña es obligatoria.'
    valido = false
  }

  return valido
}

async function manejarIngreso() {
  if (!validarCampos()) return

  cargando.value = true
  mensajeError.value = ''

  try {
    const respuesta = await cuentaServicio.ingresar({
      correo: correo.value.trim(),
      clave: clave.value
    })

    // Guardar sesión reactiva y persistir datos en localStorage
    sesion.iniciarSesion(respuesta)

    // Redirección según rol o ruta previa
    if (respuesta.rol === 'ADMIN' || respuesta.rol === 'GESTOR') {
      router.push('/catalogo')
    } else {
      router.push('/catalogo')
    }
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al iniciar sesión. Verifica tus credenciales.')
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="contenedor-autenticacion">
    <div class="tarjeta tarjeta-autenticacion">
      <div class="encabezado-autenticacion">
        <h1 class="titulo-autenticacion">Iniciar Sesión</h1>
        <p class="subtitulo-autenticacion">
          Acceso para Clientes, Gestor de Ventas y Administrador.
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

      <form class="formulario" @submit.prevent="manejarIngreso" novalidate>
        <div class="grupo-campo">
          <label for="correo" class="etiqueta">
            Correo Electrónico <span class="requerido">*</span>
          </label>
          <input
            id="correo"
            v-model="correo"
            type="email"
            class="campo-texto"
            :class="{ 'con-error': erroresValidacion.correo }"
            placeholder="ejemplo@correo.com"
            autocomplete="email"
            required
          />
          <span v-if="erroresValidacion.correo" class="texto-error">
            {{ erroresValidacion.correo }}
          </span>
        </div>

        <div class="grupo-campo">
          <label for="clave" class="etiqueta">
            Contraseña <span class="requerido">*</span>
          </label>
          <input
            id="clave"
            v-model="clave"
            type="password"
            class="campo-texto"
            :class="{ 'con-error': erroresValidacion.clave }"
            placeholder="••••••••"
            autocomplete="current-password"
            required
          />
          <span v-if="erroresValidacion.clave" class="texto-error">
            {{ erroresValidacion.clave }}
          </span>
        </div>

        <button
          type="submit"
          class="btn btn-primario btn-bloque"
          :disabled="cargando"
        >
          <span v-if="cargando">Ingresando...</span>
          <span v-else>Entrar</span>
        </button>
      </form>

      <div class="pie-autenticacion">
        <p>
          ¿No tienes una cuenta?
          <router-link to="/registro">Regístrate aquí</router-link>
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

.tarjeta-autenticacion {
  width: 100%;
  max-width: 440px;
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
</style>
