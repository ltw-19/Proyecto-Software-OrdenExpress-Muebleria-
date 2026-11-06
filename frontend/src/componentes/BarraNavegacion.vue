<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sesion } from '../estado/sesion.js'

const router = useRouter()
const menuAbierto = ref(false)

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function cerrarMenu() {
  menuAbierto.value = false
}

function salir() {
  cerrarMenu()
  sesion.cerrarSesion()
  router.push('/ingreso')
}
</script>

<template>
  <header class="barra-navegacion">
    <div class="contenedor-barra">
      <div class="marca">
        <router-link to="/" class="enlace-marca" @click="cerrarMenu">
          <span class="logo-texto">OrdenExpress</span>
          <span class="logo-subtexto">Galería de Casa</span>
        </router-link>
      </div>

      <!-- Botón hamburguesa para móviles -->
      <button
        class="boton-hamburguesa"
        type="button"
        :aria-expanded="menuAbierto"
        aria-label="Abrir menú de navegación"
        @click="alternarMenu"
      >
        <span class="linea"></span>
        <span class="linea"></span>
        <span class="linea"></span>
      </button>

      <!-- Menú principal responsive -->
      <nav :class="['menu-navegacion', { 'abierto': menuAbierto }]">
        <ul class="lista-enlaces">
          <!-- Si NO está autenticado -->
          <template v-if="!sesion.estaAutenticado.value">
            <li>
              <router-link to="/ingreso" class="enlace-nav" @click="cerrarMenu">
                Iniciar Sesión
              </router-link>
            </li>
            <li>
              <router-link to="/registro" class="enlace-nav btn-destacado" @click="cerrarMenu">
                Crear Cuenta
              </router-link>
            </li>
          </template>

          <!-- Si ESTÁ autenticado -->
          <template v-else>
            <li>
              <router-link to="/catalogo" class="enlace-nav" @click="cerrarMenu">
                Catálogo
              </router-link>
            </li>

            <!-- ADMIN y GESTOR: Inventario / Registrar Mueble -->
            <li v-if="sesion.esAdmin.value || sesion.esGestor.value">
              <router-link to="/inventario" class="enlace-nav" @click="cerrarMenu">
                Inventario
              </router-link>
            </li>

            <!-- ADMIN: Gestión de Roles -->
            <li v-if="sesion.esAdmin.value">
              <router-link to="/roles" class="enlace-nav" @click="cerrarMenu">
                Roles
              </router-link>
            </li>

            <!-- ADMIN y CLIENTE: Datos Personales (GESTOR NO LA VE según reglas) -->
            <li v-if="sesion.esAdmin.value || sesion.esCliente.value">
              <router-link to="/perfil" class="enlace-nav" @click="cerrarMenu">
                Mis Datos
              </router-link>
            </li>

            <!-- Bloque de usuario actual -->
            <li class="item-usuario">
              <div class="info-usuario">
                <span class="nombre-usuario">{{ sesion.nombreCompleto.value }}</span>
                <span :class="['insignia', `insignia-${sesion.rolActual.value?.toLowerCase()}`]">
                  {{ sesion.rolActual.value }}
                </span>
              </div>
            </li>

            <!-- Botón Cerrar Sesión -->
            <li>
              <button type="button" class="btn btn-outline btn-sm btn-salir" @click="salir">
                Salir
              </button>
            </li>
          </template>
        </ul>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.barra-navegacion {
  background-color: var(--color-superficie);
  border-bottom: 1px solid var(--color-borde);
  box-shadow: var(--sombra-suave);
  position: sticky;
  top: 0;
  z-index: 100;
}

.contenedor-barra {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.85rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.marca {
  display: flex;
  flex-direction: column;
}

.enlace-marca {
  display: flex;
  flex-direction: column;
  text-decoration: none;
}

.logo-texto {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-nogal-primario);
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.logo-subtexto {
  font-size: 0.75rem;
  color: var(--color-nogal-secundario);
  font-weight: 500;
}

.boton-hamburguesa {
  display: none;
  flex-direction: column;
  justify-content: space-around;
  width: 2rem;
  height: 2rem;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.2rem;
}

.boton-hamburguesa .linea {
  width: 100%;
  height: 2.5px;
  background-color: var(--color-nogal-primario);
  border-radius: 2px;
  transition: all 0.2s ease;
}

.menu-navegacion {
  display: flex;
  align-items: center;
}

.lista-enlaces {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.enlace-nav {
  color: var(--color-texto-principal);
  font-weight: 500;
  font-size: 0.95rem;
  padding: 0.4rem 0.6rem;
  border-radius: var(--radio-control);
  text-decoration: none;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.enlace-nav:hover,
.enlace-nav.router-link-active {
  color: var(--color-nogal-primario);
  background-color: var(--color-nogal-fondo);
}

.btn-destacado {
  background-color: var(--color-nogal-primario);
  color: #FFFFFF !important;
  padding: 0.45rem 1rem;
}

.btn-destacado:hover {
  background-color: var(--color-nogal-secundario) !important;
}

.item-usuario {
  display: flex;
  align-items: center;
  padding-left: 0.5rem;
  border-left: 1px solid var(--color-borde);
}

.info-usuario {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.15rem;
}

.nombre-usuario {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-texto-principal);
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
}

.btn-salir {
  cursor: pointer;
}

/* Modo Mobile Responsive */
@media (max-width: 860px) {
  .boton-hamburguesa {
    display: flex;
  }

  .menu-navegacion {
    display: none;
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    background-color: var(--color-superficie);
    border-bottom: 1px solid var(--color-borde);
    box-shadow: var(--sombra-elevada);
    padding: 1.25rem;
  }

  .menu-navegacion.abierto {
    display: block;
  }

  .lista-enlaces {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }

  .item-usuario {
    border-left: none;
    border-top: 1px solid var(--color-borde);
    padding-top: 0.85rem;
    padding-left: 0;
    justify-content: space-between;
  }

  .btn-salir {
    width: 100%;
    margin-top: 0.5rem;
  }
}
</style>
