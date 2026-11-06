import { ref, computed } from 'vue'

const token = ref(localStorage.getItem('token') || '')
const usuario = ref(JSON.parse(localStorage.getItem('usuario') || 'null'))

export const sesion = {
  // Estado reactivo
  token,
  usuario,

  // Getters computados
  estaAutenticado: computed(() => Boolean(token.value)),
  esAdmin: computed(() => usuario.value?.rol === 'ADMIN'),
  esGestor: computed(() => usuario.value?.rol === 'GESTOR'),
  esCliente: computed(() => usuario.value?.rol === 'CLIENTE'),
  rolActual: computed(() => usuario.value?.rol || null),
  nombreCompleto: computed(() => {
    if (!usuario.value) return ''
    const apellido = usuario.value.apellido ? ` ${usuario.value.apellido}` : ''
    return `${usuario.value.nombre}${apellido}`.trim()
  }),

  obtenerToken() {
    return token.value || localStorage.getItem('token') || ''
  },

  // Iniciar sesión y persistir en localStorage para renderizar la UI
  iniciarSesion(respuestaIngreso) {
    token.value = respuestaIngreso.token
    usuario.value = {
      id: respuestaIngreso.id,
      nombre: respuestaIngreso.nombre,
      apellido: respuestaIngreso.apellido || '',
      correo: respuestaIngreso.correo,
      telefono: respuestaIngreso.telefono || '',
      direccion: respuestaIngreso.direccion || '',
      rol: respuestaIngreso.rol
    }
    localStorage.setItem('token', token.value)
    localStorage.setItem('usuario', JSON.stringify(usuario.value))
  },

  // Actualizar datos del usuario tras edición en Datos Personales
  actualizarDatos(datosActualizados) {
    if (!usuario.value) return
    usuario.value = {
      ...usuario.value,
      nombre: datosActualizados.nombre,
      apellido: datosActualizados.apellido,
      correo: datosActualizados.correo,
      telefono: datosActualizados.telefono,
      direccion: datosActualizados.direccion || '',
      rol: datosActualizados.rol || usuario.value.rol
    }
    localStorage.setItem('usuario', JSON.stringify(usuario.value))
  },

  // Cerrar sesión
  cerrarSesion() {
    token.value = ''
    usuario.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
  }
}
