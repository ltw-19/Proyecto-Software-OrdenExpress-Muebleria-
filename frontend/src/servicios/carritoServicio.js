import api from './api.js'

/**
 * Servicio para gestionar las operaciones del Carrito de Compras (CLIENTE).
 * Conforme al contrato de backend definido en CLAUDE.md:
 * - GET    /api/carrito
 * - POST   /api/carrito/items
 * - DELETE /api/carrito/items/{idMueble}
 */
export const carritoServicio = {
  /**
   * Obtiene el carrito del cliente autenticado.
   * @returns {Promise<Object>} Carrito con lista de items, cantidadItems, totalUsd, totalBs
   */
  async consultarCarrito() {
    const respuesta = await api.get('/api/carrito')
    return respuesta.data
  },

  /**
   * Añade un ítem con su cantidad al carrito del cliente.
   * @param {string} idMueble Identificador del mueble (UUID)
   * @param {number} cantidad Número entero de unidades (mínimo 1)
   * @returns {Promise<Object>} Carrito actualizado con totales recalculados por el backend
   */
  async agregarItem(idMueble, cantidad) {
    const respuesta = await api.post('/api/carrito/items', {
      idMueble,
      cantidad: Number(cantidad)
    })
    return respuesta.data
  },

  /**
   * Elimina un ítem específico del carrito.
   * @param {string} idMueble Identificador del mueble a remover
   * @returns {Promise<Object>} Carrito actualizado con totales recalculados
   */
  async eliminarItem(idMueble) {
    const respuesta = await api.delete(`/api/carrito/items/${encodeURIComponent(idMueble)}`)
    return respuesta.data
  }
}

export default carritoServicio
