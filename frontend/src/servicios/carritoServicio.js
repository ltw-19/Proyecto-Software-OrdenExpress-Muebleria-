import { ref } from 'vue'
import api from './api.js'
import { extraerMensajeError } from './cuentaServicio.js'

// Contador reactivo global del carrito (unidades totales)
export const totalItemsCarrito = ref(0)

export const carritoServicio = {
  /**
   * Consultar el carrito del cliente en sesión.
   * GET /api/carrito
   */
  async consultar() {
    const respuesta = await api.get('/api/carrito')
    totalItemsCarrito.value = respuesta.data?.cantidadItems ?? 0
    return respuesta.data
  },

  /**
   * Añadir un mueble al carrito con una cantidad específica.
   * POST /api/carrito/items
   * @param {string} idMueble
   * @param {number} cantidad
   */
  async agregarItem(idMueble, cantidad) {
    const respuesta = await api.post('/api/carrito/items', {
      idMueble,
      cantidad: parseInt(cantidad, 10)
    })
    totalItemsCarrito.value = respuesta.data?.cantidadItems ?? 0
    return respuesta.data
  },

  /**
   * Eliminar un mueble del carrito.
   * DELETE /api/carrito/items/{idMueble}
   * @param {string} idMueble
   */
  async eliminarItem(idMueble) {
    const respuesta = await api.delete(`/api/carrito/items/${idMueble}`)
    totalItemsCarrito.value = respuesta.data?.cantidadItems ?? 0
    return respuesta.data
  },

  /**
   * Actualiza el contador local de unidades en carrito
   */
  actualizarContador(conteo) {
    totalItemsCarrito.value = conteo
  }
}

export { extraerMensajeError }