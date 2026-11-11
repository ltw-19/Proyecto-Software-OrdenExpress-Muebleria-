import { ref, computed } from 'vue'
import { carritoServicio } from '../servicios/carritoServicio.js'
import { sesion } from './sesion.js'

const items = ref([])
const totalUsd = ref(0)
const totalBs = ref(0)
const cargando = ref(false)

// Conforme a CLAUDE.md: "El contador del carrito cuenta unidades"
const conteoUnidades = computed(() => {
  return items.value.reduce((acumulado, item) => acumulado + (Number(item.cantidad) || 0), 0)
})

export const carritoEstado = {
  items,
  totalUsd,
  totalBs,
  cargando,
  conteoUnidades,

  /**
   * Actualiza el estado reactivo con la respuesta recibida del backend.
   */
  sincronizarConRespuesta(carritoRespuesta) {
    if (!carritoRespuesta) {
      items.value = []
      totalUsd.value = 0
      totalBs.value = 0
      return
    }
    items.value = Array.isArray(carritoRespuesta.items) ? carritoRespuesta.items : []
    totalUsd.value = carritoRespuesta.totalUsd ?? 0
    totalBs.value = carritoRespuesta.totalBs ?? 0
  },

  /**
   * Consulta el carrito desde el backend solo si hay una sesión activa de CLIENTE.
   */
  async cargarCarrito() {
    if (!sesion.estaAutenticado.value || !sesion.esCliente.value) {
      this.vaciar()
      return
    }

    cargando.value = true
    try {
      const respuesta = await carritoServicio.consultarCarrito()
      this.sincronizarConRespuesta(respuesta)
    } catch {
      // Si falla la consulta no rompemos la UI; se deja en estado limpio
      items.value = []
      totalUsd.value = 0
      totalBs.value = 0
    } finally {
      cargando.value = false
    }
  },

  /**
   * Limpia el carrito reactivo (tras registrar pedido o cerrar sesión).
   */
  vaciar() {
    items.value = []
    totalUsd.value = 0
    totalBs.value = 0
  }
}

export default carritoEstado
