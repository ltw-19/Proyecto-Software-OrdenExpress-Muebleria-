import api from './api.js'

/**
 * Servicio para el módulo de cuentas, autenticación y gestión de roles.
 */
export const cuentaServicio = {
  /**
   * Iniciar sesión en el sistema.
   * POST /api/cuentas/ingresar
   * @param {{correo: string, clave: string}} credenciales
   * @returns {Promise<{token, id, nombre, apellido, correo, telefono, direccion, rol}>}
   */
  async ingresar(credenciales) {
    const respuesta = await api.post('/api/cuentas/ingresar', credenciales)
    return respuesta.data
  },

  /**
   * Registrar una nueva cuenta de cliente.
   * POST /api/cuentas/registro
   * @param {{nombre: string, apellido: string, correo: string, clave: string, telefono: string, direccion?: string}} datos
   * @returns {Promise<{id, nombre, apellido, correo, telefono, direccion, rol}>}
   */
  async registrar(datos) {
    const respuesta = await api.post('/api/cuentas/registro', datos)
    return respuesta.data
  },

  /**
   * Modificar datos personales del usuario actual (ADMIN y CLIENTE).
   * PUT /api/cuentas/yo
   * @param {{nombre: string, apellido: string, correo: string, telefono: string, direccion?: string}} datos
   * @returns {Promise<{id, nombre, apellido, correo, telefono, direccion, rol}>}
   */
  async modificarMisDatos(datos) {
    const respuesta = await api.put('/api/cuentas/yo', datos)
    return respuesta.data
  },

  /**
   * Listar todas las cuentas para gestión de roles (solo ADMIN).
   * GET /api/cuentas
   * @returns {Promise<Array<{id, nombre, apellido, correo, telefono, direccion, rol}>>}
   */
  async listarCuentas() {
    const respuesta = await api.get('/api/cuentas')
    return respuesta.data
  },

  /**
   * Cambiar el rol de una cuenta (solo ADMIN, no puede cambiarse el suyo).
   * PUT /api/cuentas/{id}/rol
   * @param {string} idCuenta
   * @param {'ADMIN'|'GESTOR'|'CLIENTE'} nuevoRol
   * @returns {Promise<{id, nombre, apellido, correo, telefono, direccion, rol}>}
   */
  async cambiarRol(idCuenta, nuevoRol) {
    const respuesta = await api.put(`/api/cuentas/${idCuenta}/rol`, { rol: nuevoRol })
    return respuesta.data
  }
}

/**
 * Función utilitaria para extraer el mensaje de error del backend.
 * Siempre respeta respuesta.data.mensaje según la convención del proyecto.
 */
export function extraerMensajeError(error, mensajePorDefecto = 'Ha ocurrido un error inesperado') {
  if (error.response && error.response.data && error.response.data.mensaje) {
    return error.response.data.mensaje
  }
  // Si no hay respuesta del servidor (servidor apagado o error de red)
  if (error.message === 'Network Error' || !error.response) {
    return 'No se pudo conectar con el servidor backend. Asegúrate de que el backend de Spring Boot esté iniciado en http://localhost:8080.'
  }
  if (error.message) {
    return error.message
  }
  return mensajePorDefecto
}