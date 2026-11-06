<script setup>
import { ref, onMounted } from 'vue'
import { sesion } from '../estado/sesion.js'
import { cuentaServicio, extraerMensajeError } from '../servicios/cuentaServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const cuentas = ref([])
const cargando = ref(false)
const cambiandoRolId = ref(null)
const mensajeError = ref('')
const mensajeExito = ref('')

onMounted(() => {
  cargarCuentas()
})

async function cargarCuentas() {
  cargando.value = true
  mensajeError.value = ''
  try {
    cuentas.value = await cuentaServicio.listarCuentas()
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al cargar la lista de cuentas.')
  } finally {
    cargando.value = false
  }
}

async function cambiarRolCuenta(cuenta, nuevoRol) {
  if (cuenta.id === sesion.usuario.value?.id) {
    mensajeError.value = 'No puedes modificar tu propio rol de Administrador.'
    return
  }

  cambiandoRolId.value = cuenta.id
  mensajeError.value = ''
  mensajeExito.value = ''

  try {
    const actualizada = await cuentaServicio.cambiarRol(cuenta.id, nuevoRol)
    cuenta.rol = actualizada.rol
    mensajeExito.value = `Se actualizó el rol de ${cuenta.nombre} ${cuenta.apellido} a ${nuevoRol}.`
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo actualizar el rol.')
    // Revertir en caso de fallo volviendo a consultar la lista
    await cargarCuentas()
  } finally {
    cambiandoRolId.value = null
  }
}
</script>

<template>
  <div class="contenedor">
    <div class="encabezado-seccion">
      <div>
        <h1>Gestión de Roles</h1>
        <p>Administra los permisos y roles de los usuarios del sistema.</p>
      </div>
      <button class="btn btn-outline" :disabled="cargando" @click="cargarCuentas">
        ↻ Actualizar
      </button>
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

    <div v-if="cargando && cuentas.length === 0" class="cargando">
      Cargando cuentas...
    </div>

    <div v-else class="tarjeta contenedor-tabla">
      <!-- Versión para escritorio: Tabla -->
      <table class="tabla-roles">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Teléfono</th>
            <th>Rol Actual</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="cuenta in cuentas"
            :key="cuenta.id"
            :class="{ 'fila-propia': cuenta.id === sesion.usuario.value?.id }"
          >
            <td>
              <strong>{{ cuenta.nombre }} {{ cuenta.apellido }}</strong>
              <span v-if="cuenta.id === sesion.usuario.value?.id" class="etiqueta-tu-cuenta">
                (Tu cuenta)
              </span>
            </td>
            <td>{{ cuenta.correo }}</td>
            <td>{{ cuenta.telefono }}</td>
            <td>
              <span :class="['insignia', `insignia-${cuenta.rol.toLowerCase()}`]">
                {{ cuenta.rol }}
              </span>
            </td>
            <td>
              <!-- Selector de rol deshabilitado para el propio ADMIN -->
              <select
                :value="cuenta.rol"
                class="campo-selector selector-rol"
                :disabled="cuenta.id === sesion.usuario.value?.id || cambiandoRolId === cuenta.id"
                @change="cambiarRolCuenta(cuenta, $event.target.value)"
              >
                <option value="CLIENTE">CLIENTE</option>
                <option value="GESTOR">GESTOR</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Versión Móvil: Tarjetas individuales para garantizar usabilidad mobile-first -->
      <div class="lista-tarjetas-movil">
        <div
          v-for="cuenta in cuentas"
          :key="cuenta.id"
          class="tarjeta-cuenta-movil"
          :class="{ 'borde-propio': cuenta.id === sesion.usuario.value?.id }"
        >
          <div class="cabecera-tarjeta-movil">
            <div>
              <h3>{{ cuenta.nombre }} {{ cuenta.apellido }}</h3>
              <span v-if="cuenta.id === sesion.usuario.value?.id" class="etiqueta-tu-cuenta">
                (Tu cuenta)
              </span>
            </div>
            <span :class="['insignia', `insignia-${cuenta.rol.toLowerCase()}`]">
              {{ cuenta.rol }}
            </span>
          </div>

          <div class="detalles-movil">
            <p><strong>Correo:</strong> {{ cuenta.correo }}</p>
            <p><strong>Teléfono:</strong> {{ cuenta.telefono }}</p>
          </div>

          <div class="pie-tarjeta-movil">
            <label class="etiqueta">Asignar rol:</label>
            <select
              :value="cuenta.rol"
              class="campo-selector"
              :disabled="cuenta.id === sesion.usuario.value?.id || cambiandoRolId === cuenta.id"
              @change="cambiarRolCuenta(cuenta, $event.target.value)"
            >
              <option value="CLIENTE">CLIENTE</option>
              <option value="GESTOR">GESTOR</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.encabezado-seccion {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.cargando {
  text-align: center;
  padding: 2rem;
  color: var(--color-texto-secundario);
}

.contenedor-tabla {
  padding: 0;
  overflow: hidden;
}

.tabla-roles {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.tabla-roles th,
.tabla-roles td {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-borde);
  font-size: 0.95rem;
}

.tabla-roles th {
  background-color: var(--color-nogal-fondo);
  color: var(--color-nogal-primario);
  font-weight: 600;
}

.fila-propia {
  background-color: #FAFAFA;
}

.etiqueta-tu-cuenta {
  font-size: 0.8rem;
  color: var(--color-nogal-secundario);
  font-weight: normal;
  margin-left: 0.4rem;
}

.selector-rol {
  max-width: 140px;
  padding: 0.45rem 0.65rem;
  font-size: 0.875rem;
}

/* Modo Mobile: ocultar tabla y mostrar tarjetas */
.lista-tarjetas-movil {
  display: none;
  padding: 1rem;
  flex-direction: column;
  gap: 1rem;
}

.tarjeta-cuenta-movil {
  border: 1px solid var(--color-borde);
  border-radius: var(--radio-control);
  padding: 1rem;
  background-color: var(--color-superficie);
}

.tarjeta-cuenta-movil.borde-propio {
  border-left: 4px solid var(--color-nogal-primario);
}

.cabecera-tarjeta-movil {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.detalles-movil p {
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
}

.pie-tarjeta-movil {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-borde);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .tabla-roles {
    display: none;
  }
  .lista-tarjetas-movil {
    display: flex;
  }
}
</style>
