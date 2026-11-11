import api from './api.js'

/**
 * Servicio para gestionar las operaciones de Pedidos.
 * Conforme al contrato de backend definido en CLAUDE.md:
 * - POST   /api/pedidos                (CLIENTE: registrar pedido con datos de entrega)
 * - GET    /api/pedidos/{id}/preorden  (CLIENTE: descargar PDF de preorden)
 * - GET    /api/pedidos                (TODOS: cliente los suyos, admin/gestor todos)
 * - GET    /api/pedidos/{id}           (TODOS: consultar detalle)
 * - PUT    /api/pedidos/{id}/aprobar   (GESTOR)
 * - PUT    /api/pedidos/{id}/rechazar  (GESTOR)
 * - DELETE /api/pedidos/{id}           (CLIENTE solo pendientes, GESTOR cualquiera)
 */
export const pedidoServicio = {
  /**
   * Registra un nuevo pedido a partir de los ítems en el carrito del cliente.
   * @param {Object} datosEntrega { nombre, apellido, telefono, direccion }
   * @returns {Promise<Object>} Pedido registrado con enlaceWhatsapp
   */
  async registrarPedido(datosEntrega) {
    const respuesta = await api.post('/api/pedidos', {
      nombre: datosEntrega.nombre?.trim(),
      apellido: datosEntrega.apellido?.trim(),
      telefono: datosEntrega.telefono?.trim(),
      direccion: datosEntrega.direccion?.trim()
    })
    return respuesta.data
  },

  /**
   * Lista los pedidos (del más reciente al más antiguo).
   * - CLIENTE: solo sus pedidos.
   * - ADMIN / GESTOR: bandeja global.
   * @returns {Promise<Array>} Lista de pedidos
   */
  async consultarPedidos() {
    const respuesta = await api.get('/api/pedidos')
    return respuesta.data
  },

  /**
   * Obtiene el detalle de un pedido por su identificador único.
   * @param {string} id UUID del pedido
   * @returns {Promise<Object>} Detalle completo del pedido
   */
  async consultarDetalle(id) {
    const respuesta = await api.get(`/api/pedidos/${encodeURIComponent(id)}`)
    return respuesta.data
  },

  /**
   * Aprueba un pedido pendiente (solo rol GESTOR).
   * @param {string} id UUID del pedido
   * @returns {Promise<Object>} Pedido actualizado en estado APROBADO
   */
  async aprobarPedido(id) {
    const respuesta = await api.put(`/api/pedidos/${encodeURIComponent(id)}/aprobar`)
    return respuesta.data
  },

  /**
   * Rechaza un pedido pendiente (solo rol GESTOR).
   * @param {string} id UUID del pedido
   * @returns {Promise<Object>} Pedido actualizado en estado RECHAZADO
   */
  async rechazarPedido(id) {
    const respuesta = await api.put(`/api/pedidos/${encodeURIComponent(id)}/rechazar`)
    return respuesta.data
  },

  /**
   * Elimina un pedido.
   * - CLIENTE: solo si está PENDIENTE y le pertenece.
   * - GESTOR: en cualquier estado.
   * @param {string} id UUID del pedido
   * @returns {Promise<void>}
   */
  async eliminarPedido(id) {
    await api.delete(`/api/pedidos/${encodeURIComponent(id)}`)
  },

  /**
   * Descarga el documento PDF de la Pre-orden.
   * Se pide como 'blob' y se crea el enlace temporal para descargarlo con el nombre correcto.
   * @param {string} id UUID del pedido
   * @param {string} numero Número legible del pedido (ej. OE-0001)
   */
  async descargarPreordenPdf(id, numero = '0000') {
    const respuesta = await api.get(`/api/pedidos/${encodeURIComponent(id)}/preorden`, {
      responseType: 'blob'
    })

    const blob = new Blob([respuesta.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = `preorden-${numero}.pdf`
    document.body.appendChild(enlace)
    enlace.click()
    document.body.removeChild(enlace)
    window.URL.revokeObjectURL(url)
  }
}

export default pedidoServicio
