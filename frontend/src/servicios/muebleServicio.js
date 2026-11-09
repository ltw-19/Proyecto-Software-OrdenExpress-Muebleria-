import api from './api.js'
import { extraerMensajeError } from './cuentaServicio.js'

/**
 * Servicio para el módulo de catálogo e inventario de muebles.
 */
export const muebleServicio = {
  /**
   * Consultar catálogo de muebles con filtros opcionales de búsqueda y categoría.
   * GET /api/muebles?busqueda=&categoria=
   * @param {{busqueda?: string, categoria?: string}} [filtros={}]
   * @returns {Promise<Array<{id: string, codigo: string, nombre: string, categoria: string, descripcion: string, dimensiones: {ancho: number, alto: number, profundidad: number}, material: string, urlFoto: string, precioUsd: number, precioBs: number, disponible: boolean, unidades?: number}>>}
   */
  async obtenerMuebles(filtros = {}) {
    const params = {}
    if (filtros.busqueda && filtros.busqueda.trim()) {
      params.busqueda = filtros.busqueda.trim()
    }
    if (filtros.categoria && filtros.categoria.trim() && filtros.categoria !== 'TODAS') {
      params.categoria = filtros.categoria.trim()
    }

    const respuesta = await api.get('/api/muebles', { params })
    return respuesta.data
  },

  /**
   * Obtener la lista de nombres de categorías registradas en el sistema para el filtro.
   * GET /api/muebles/categorias
   * @returns {Promise<string[]>}
   */
  async obtenerCategorias() {
    const respuesta = await api.get('/api/muebles/categorias')
    return respuesta.data
  },

  /**
   * Registrar un nuevo mueble en el inventario (solo ADMIN y GESTOR).
   * POST /api/muebles
   * @param {{nombre: string, categoria: string, descripcion: string, dimensiones: {ancho: number, alto: number, profundidad: number}, material: string, urlFoto: string, precioUsd: number, unidades: number}} datos
   * @returns {Promise<Object>}
   */
  async registrarMueble(datos) {
    const respuesta = await api.post('/api/muebles', datos)
    return respuesta.data
  }
}

export { extraerMensajeError }
