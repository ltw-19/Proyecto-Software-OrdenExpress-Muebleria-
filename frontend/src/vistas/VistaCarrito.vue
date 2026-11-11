<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sesion } from '../estado/sesion.js'
import { carritoServicio, extraerMensajeError } from '../servicios/carritoServicio.js'
import { pedidoServicio } from '../servicios/pedidoServicio.js'
import AlertaMensaje from '../componentes/AlertaMensaje.vue'

const router = useRouter()

const carrito = ref(null)
const cargando = ref(false)
const procesandoPedido = ref(false)
const eliminandoId = ref(null)
const mensajeError = ref('')
const mensajeExito = ref('')

// Pedido recién creado tras el registro exitoso
const pedidoCreado = ref(null)

// Formulario de datos de entrega
const entrega = ref({
  nombre: '',
  apellido: '',
  telefono: '',
  direccion: ''
})

const erroresValidacion = ref({
  nombre: '',
  apellido: '',
  telefono: '',
  direccion: ''
})

onMounted(async () => {
  precargarDatosEntrega()
  await cargarCarrito()
})

function precargarDatosEntrega() {
  if (sesion.usuario.value) {
    entrega.value = {
      nombre: sesion.usuario.value.nombre || '',
      apellido: sesion.usuario.value.apellido || '',
      telefono: sesion.usuario.value.telefono || '',
      direccion: sesion.usuario.value.direccion || ''
    }
  }
}

async function cargarCarrito() {
  cargando.value = true
  mensajeError.value = ''
  try {
    carrito.value = await carritoServicio.consultar()
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'No se pudo cargar el carrito.')
    carrito.value = null
  } finally {
    cargando.value = false
  }
}

async function eliminarItem(idMueble) {
  eliminandoId.value = idMueble
  mensajeError.value = ''
  mensajeExito.value = ''
  try {
    carrito.value = await carritoServicio.eliminarItem(idMueble)
    mensajeExito.value = 'Mueble eliminado del carrito.'
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al eliminar el ítem del carrito.')
  } finally {
    eliminandoId.value = null
  }
}

function limpiarTelefono(tel) {
  return tel.replace(/[\s\-\(\)]/g, '')
}

function validarEntrega() {
  let valido = true
  erroresValidacion.value = { nombre: '', apellido: '', telefono: '', direccion: '' }
  mensajeError.value = ''

  if (!entrega.value.nombre.trim()) {
    erroresValidacion.value.nombre = 'El nombre es obligatorio.'
    valido = false
  }

  if (!entrega.value.apellido.trim()) {
    erroresValidacion.value.apellido = 'El apellido es obligatorio.'
    valido = false
  }

  const telLimpio = limpiarTelefono(entrega.value.telefono)
  if (!telLimpio) {
    erroresValidacion.value.telefono = 'El teléfono es obligatorio.'
    valido = false
  } else if (!/^\+?[0-9]{10,15}$/.test(telLimpio)) {
    erroresValidacion.value.telefono = 'El teléfono debe tener entre 10 y 15 dígitos.'
    valido = false
  }

  if (!entrega.value.direccion.trim()) {
    erroresValidacion.value.direccion = 'La dirección de entrega es obligatoria para procesar el pedido.'
    valido = false
  } else if (entrega.value.direccion.trim().length > 200) {
    erroresValidacion.value.direccion = 'La dirección no puede exceder los 200 caracteres.'
    valido = false
  }

  return valido
}

async function registrarPedido() {
  if (!validarEntrega()) return

  procesandoPedido.value = true
  mensajeError.value = ''

  try {
    const payload = {
      nombre: entrega.value.nombre.trim(),
      apellido: entrega.value.apellido.trim(),
      telefono: limpiarTelefono(entrega.value.telefono),
      direccion: entrega.value.direccion.trim()
    }

    const respuesta = await pedidoServicio.registrar(payload)
    pedidoCreado.value = respuesta
    // Actualizamos el carrito local ya que el backend lo vacía al registrar
    carritoServicio.actualizarContador(0)
    carrito.value = null
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al registrar el pedido.')
  } finally {
    procesandoPedido.value = false
  }
}

async function descargarPdf(pedido) {
  try {
    await pedidoServicio.descargarPreorden(pedido.id, pedido.numero)
  } catch (error) {
    mensajeError.value = extraerMensajeError(error, 'Error al descargar la Pre-orden en PDF.')
  }
}

function abrirWhatsapp(enlace) {
  if (enlace) {
    window.open(enlace, '_blank')
  }
}

function formatoMoneda(valor) {
  if (valor == null || isNaN(valor)) return '0,00'
  return new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(valor)
}
</script>

<template>
  <div class="contenedor vista-carrito">
    <header class="encabezado-carrito">
      <h1>Tu Carrito de Compras</h1>
      <p>Revisa los muebles seleccionados y completa tus datos de entrega para armar tu pre-orden.</p>
    </header>

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

    <!-- Estado: Pedido Confirmado Exitosamente -->
    <div v-if="pedidoCreado" class="tarjeta tarjeta-exito-pedido">
      <div class="icono-confirmacion">🎉</div>
      <h2>¡Pedido Registrado con Éxito!</h2>
      <p class="numero-pedido">
        Número de Pedido: <strong>{{ pedidoCreado.numero }}</strong>
      </p>
      <p class="instruccion-pedido">
        Tu pedido ha sido creado en estado <strong>PENDIENTE</strong>. Para finalizar tu compra, descarga tu comprobante de Pre-orden en PDF y comunícate con nuestro Gestor de Ventas por WhatsApp para coordinar el pago y la entrega.
      </p>

      <div class="acciones-pedido-creado">
        <button
          type="button"
          class="btn btn-outline"
          @click="descargarPdf(pedidoCreado)"
        >
          📄 Descargar Pre-orden (PDF)
        </button>

        <!-- Botón verde #25D366 reservado EXCLUSIVAMENTE para WhatsApp -->
        <button
          type="button"
          class="btn btn-whatsapp"
          @click="abrirWhatsapp(pedidoCreado.enlaceWhatsapp)"
        >
          📱 Enviar Pedido a WhatsApp
        </button>
      </div>

      <div class="enlaces-adicionales">
        <router-link to="/mis-pedidos" class="enlace-mis-pedidos">
          Ir a Mis Pedidos
        </router-link>
        <span class="separador">|</span>
        <router-link to="/catalogo" class="enlace-catalogo">
          Seguir explorando el Catálogo
        </router-link>
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-else-if="cargando" class="cargando">
      Cargando tu carrito...
    </div>

    <!-- Estado: Carrito Vacío -->
    <div v-else-if="!carrito || !carrito.items || carrito.items.length === 0" class="tarjeta carrito-vacio">
      <div class="icono-vacio">🛒</div>
      <h2>Tu carrito está vacío</h2>
      <p>Aún no has agregado muebles a tu carrito de compras.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Explorar Catálogo
      </router-link>
    </div>

    <!-- Estado: Carrito con Ítems -->
    <div v-else class="cuadricula-carrito">
      <!-- Columna izquierda: Lista de Ítems (Solo lectura de cantidad) -->
      <section class="tarjeta seccion-items">
        <h2 class="subtitulo-seccion">Ítems Seleccionados ({{ carrito.cantidadItems }})</h2>

        <div class="lista-items">
          <div
            v-for="item in carrito.items"
            :key="item.idMueble"
            class="fila-item"
          >
            <div class="foto-miniatura">
              <img :src="item.urlFoto" :alt="item.nombre" />
            </div>

            <div class="info-item">
              <div class="cabecera-item">
                <span class="codigo-mueble">{{ item.codigo }}</span>
                <span v-if="!item.disponible" class="insignia-agotado">Agotado</span>
              </div>
              <h3 class="nombre-item">{{ item.nombre }}</h3>
              <p class="precio-unitario">
                $ {{ formatoMoneda(item.precioUsd) }} / Bs. {{ formatoMoneda(item.precioBs) }} c/u
              </p>
            </div>

            <div class="cantidad-item">
              <span class="etiqueta-cantidad">Cantidad:</span>
              <!-- REGLA DE NEGOCIO: Cantidad de SOLO LECTURA (sin botones - / +) -->
              <span class="cifra-cantidad">{{ item.cantidad }}</span>
            </div>

            <div class="subtotales-item">
              <span class="subtotal-usd">$ {{ formatoMoneda(item.subtotalUsd) }}</span>
              <span class="subtotal-bs">Bs. {{ formatoMoneda(item.subtotalBs) }}</span>
            </div>

            <div class="accion-eliminar">
              <button
                type="button"
                class="btn-eliminar-item"
                :disabled="eliminandoId === item.idMueble"
                title="Eliminar este mueble del carrito"
                @click="eliminarItem(item.idMueble)"
              >
                &times;
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Columna derecha: Resumen y Datos de Entrega -->
      <aside class="columna-resumen">
        <div class="tarjeta tarjeta-resumen">
          <h2 class="subtitulo-seccion">Resumen de Pre-orden</h2>

          <div class="lineas-totales">
            <div class="linea-total">
              <span>Total USD:</span>
              <strong class="monto-usd">$ {{ formatoMoneda(carrito.totalUsd) }}</strong>
            </div>
            <div class="linea-total">
              <span>Total Estimado en Bs:</span>
              <strong class="monto-bs">Bs. {{ formatoMoneda(carrito.totalBs) }}</strong>
            </div>
            <small class="texto-tasa-aviso">
              Calculado según la tasa oficial del BCV al registrarse el pedido.
            </small>
          </div>

          <hr class="divisor" />

          <!-- Formulario de Entrega -->
          <form class="formulario-entrega" @submit.prevent="registrarPedido" novalidate>
            <h3 class="titulo-entrega">Datos para la Entrega</h3>

            <div class="fila-doble">
              <div class="grupo-campo">
                <label for="entrega-nombre" class="etiqueta">
                  Nombre <span class="requerido">*</span>
                </label>
                <input
                  id="entrega-nombre"
                  v-model="entrega.nombre"
                  type="text"
                  class="campo-texto"
                  :class="{ 'con-error': erroresValidacion.nombre }"
                  required
                />
                <span v-if="erroresValidacion.nombre" class="texto-error">
                  {{ erroresValidacion.nombre }}
                </span>
              </div>

              <div class="grupo-campo">
                <label for="entrega-apellido" class="etiqueta">
                  Apellido <span class="requerido">*</span>
                </label>
                <input
                  id="entrega-apellido"
                  v-model="entrega.apellido"
                  type="text"
                  class="campo-texto"
                  :class="{ 'con-error': erroresValidacion.apellido }"
                  required
                />
                <span v-if="erroresValidacion.apellido" class="texto-error">
                  {{ erroresValidacion.apellido }}
                </span>
              </div>
            </div>

            <div class="grupo-campo">
              <label for="entrega-telefono" class="etiqueta">
                Teléfono de Contacto <span class="requerido">*</span>
              </label>
              <input
                id="entrega-telefono"
                v-model="entrega.telefono"
                type="tel"
                class="campo-texto"
                :class="{ 'con-error': erroresValidacion.telefono }"
                placeholder="04121234567"
                required
              />
              <span v-if="erroresValidacion.telefono" class="texto-error">
                {{ erroresValidacion.telefono }}
              </span>
            </div>

            <div class="grupo-campo">
              <label for="entrega-direccion" class="etiqueta">
                Dirección Exacta de Entrega <span class="requerido">*</span>
              </label>
              <textarea
                id="entrega-direccion"
                v-model="entrega.direccion"
                rows="2"
                class="campo-area"
                :class="{ 'con-error': erroresValidacion.direccion }"
                maxlength="200"
                placeholder="Calle, número de casa, punto de referencia"
                required
              ></textarea>
              <span v-if="erroresValidacion.direccion" class="texto-error">
                {{ erroresValidacion.direccion }}
              </span>
              <span class="texto-ayuda">{{ entrega.direccion.length }}/200 caracteres</span>
            </div>

            <button
              type="submit"
              class="btn btn-primario btn-bloque"
              :disabled="procesandoPedido"
            >
              <span v-if="procesandoPedido">Procesando pre-orden...</span>
              <span v-else>Confirmar y Registrar Pedido</span>
            </button>
          </form>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.vista-carrito {
  padding-top: 1.5rem;
  padding-bottom: 3rem;
}

.encabezado-carrito {
  margin-bottom: 1.5rem;
}

.cargando {
  text-align: center;
  padding: 3rem;
  color: var(--color-texto-secundario);
}

.carrito-vacio {
  text-align: center;
  padding: 3.5rem 1.5rem;
  max-width: 500px;
  margin: 2rem auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.icono-vacio {
  font-size: 3.5rem;
}

.cuadricula-carrito {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 1.5rem;
  align-items: flex-start;
}

.seccion-items {
  padding: 1.5rem;
}

.subtitulo-seccion {
  font-size: 1.2rem;
  margin-bottom: 1.25rem;
  color: var(--color-nogal-primario);
}

.lista-items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.fila-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-borde);
}

.fila-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.foto-miniatura {
  width: 75px;
  height: 65px;
  border-radius: var(--radio-control);
  overflow: hidden;
  background-color: var(--color-nogal-fondo);
  flex-shrink: 0;
}

.foto-miniatura img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-item {
  flex: 1;
}

.cabecera-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.2rem;
}

.codigo-mueble {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-nogal-secundario);
}

.insignia-agotado {
  font-size: 0.7rem;
  background-color: var(--color-error-fondo);
  color: var(--color-error);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.nombre-item {
  font-size: 1rem;
  margin-bottom: 0.2rem;
  color: var(--color-texto-principal);
}

.precio-unitario {
  font-size: 0.8rem;
  color: var(--color-texto-mutado);
  margin-bottom: 0;
}

.cantidad-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0 0.5rem;
}

.etiqueta-cantidad {
  font-size: 0.75rem;
  color: var(--color-texto-mutado);
}

.cifra-cantidad {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--color-nogal-primario);
}

.subtotales-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 90px;
}

.subtotal-usd {
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-nogal-primario);
}

.subtotal-bs {
  font-size: 0.8rem;
  color: var(--color-texto-secundario);
}

.btn-eliminar-item {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  color: var(--color-error);
  cursor: pointer;
  line-height: 1;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.btn-eliminar-item:hover:not(:disabled) {
  background-color: var(--color-error-fondo);
}

.columna-resumen {
  position: sticky;
  top: 80px;
}

.tarjeta-resumen {
  padding: 1.5rem;
}

.lineas-totales {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-bottom: 1rem;
}

.linea-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 0.95rem;
}

.monto-usd {
  font-size: 1.35rem;
  color: var(--color-nogal-primario);
}

.monto-bs {
  font-size: 1.1rem;
  color: var(--color-texto-principal);
}

.texto-tasa-aviso {
  color: var(--color-texto-mutado);
  font-size: 0.75rem;
  display: block;
}

.divisor {
  border: none;
  border-top: 1px solid var(--color-borde);
  margin: 1.25rem 0;
}

.titulo-entrega {
  font-size: 1.05rem;
  margin-bottom: 0.85rem;
  color: var(--color-nogal-primario);
}

.fila-doble {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.btn-bloque {
  width: 100%;
  padding: 0.85rem;
  margin-top: 1rem;
  font-size: 1rem;
}

/* Tarjeta de Pedido Confirmado */
.tarjeta-exito-pedido {
  max-width: 620px;
  margin: 2rem auto;
  text-align: center;
  padding: 2.5rem 1.75rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.icono-confirmacion {
  font-size: 3.5rem;
}

.numero-pedido {
  font-size: 1.2rem;
  color: var(--color-nogal-primario);
}

.instruccion-pedido {
  font-size: 0.95rem;
  line-height: 1.6;
  max-width: 520px;
}

.acciones-pedido-creado {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
  margin-top: 0.75rem;
}

.enlaces-adicionales {
  margin-top: 1.5rem;
  display: flex;
  gap: 1rem;
  font-size: 0.9rem;
}

.separador {
  color: var(--color-borde);
}

@media (max-width: 860px) {
  .cuadricula-carrito {
    grid-template-columns: 1fr;
  }

  .columna-resumen {
    position: static;
  }

  .fila-item {
    flex-wrap: wrap;
  }
}
</style>