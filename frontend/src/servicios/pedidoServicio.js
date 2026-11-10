import api from './api.js'
import { extraerMensajeError } from './cuentaServicio.js'

export const pedidoServicio = {
  /**
   * Registrar pedido a partir del carrito guardado en el backend.
   * POST /api/pedidos
   * @param {{nombre: string, apellido: string, telefono: string, direccion: string}} datosEntrega
   */
  async registrar(datosEntrega) {
    const respuesta = await api.post('/api/pedidos', datosEntrega)
    return respuesta.data
  },

  /**
   * Consultar lista de pedidos según el rol:
   * CLIENTE: solo sus pedidos.
   * ADMIN y GESTOR: todos los pedidos del sistema.
   * GET /api/pedidos
   */
  async consultar() {
    const respuesta = await api.get('/api/pedidos')
    return respuesta.data
  },

  /**
   * Consultar detalle de un pedido específico.
   * GET /api/pedidos/{id}
   */
  async consultarDetalle(id) {
    const respuesta = await api.get(`/api/pedidos/${id}`)
    return respuesta.data
  },

  /**
   * Descargar el archivo PDF de la Pre-orden oficial.
   * GET /api/pedidos/{id}/preorden
   * @param {string} id
   * @param {string} [numero='']
   */
  async descargarPreorden(id, numero = '') {
    const respuesta = await api.get(`/api/pedidos/${id}/preorden`, {
      responseType: 'blob'
    })
    const blob = new Blob([respuesta.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = `preorden-${numero || id}.pdf`
    document.body.appendChild(enlace)
    enlace.click()
    document.body.removeChild(enlace)
    window.URL.revokeObjectURL(url)
  },

  /**
   * Aprobar pedido y descontar stock automáticamente (solo GESTOR).
   * PUT /api/pedidos/{id}/aprobar
   */
  async aprobar(id) {
    const respuesta = await api.put(`/api/pedidos/${id}/aprobar`)
    return respuesta.data
  },

  /**
   * Rechazar pedido sin tocar stock (solo GESTOR).
   * PUT /api/pedidos/{id}/rechazar
   */
  async rechazar(id) {
    const respuesta = await api.put(`/api/pedidos/${id}/rechazar`)
    return respuesta.data
  },

  /**
   * Eliminar pedido:
   * CLIENTE: solo los suyos y solo si está en estado PENDIENTE.
   * GESTOR: cualquiera.
   * ADMIN: prohibido (403).
   * DELETE /api/pedidos/{id}
   */
  async eliminar(id) {
    await api.delete(`/api/pedidos/${id}`)
  }
}

export { extraerMensajeError }