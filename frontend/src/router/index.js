import { createRouter, createWebHistory } from 'vue-router'
import { sesion } from '../estado/sesion.js'

// Vistas
import VistaIngreso from '../vistas/VistaIngreso.vue'
import VistaRegistro from '../vistas/VistaRegistro.vue'
import VistaDatosPersonales from '../vistas/VistaDatosPersonales.vue'
import VistaGestionRoles from '../vistas/VistaGestionRoles.vue'
import VistaCatalogo from '../vistas/VistaCatalogo.vue'
import VistaInventario from '../vistas/VistaInventario.vue'
import VistaNoAutorizado from '../vistas/VistaNoAutorizado.vue'

const routes = [
  {
    path: '/',
    redirect: () => {
      return sesion.estaAutenticado.value ? '/catalogo' : '/ingreso'
    }
  },
  {
    path: '/ingreso',
    name: 'Ingreso',
    component: VistaIngreso,
    meta: { publica: true }
  },
  {
    path: '/registro',
    name: 'Registro',
    component: VistaRegistro,
    meta: { publica: true }
  },
  {
    path: '/catalogo',
    name: 'Catalogo',
    component: VistaCatalogo,
    meta: { requiereAutenticacion: true }
  },
  {
    path: '/inventario',
    name: 'Inventario',
    component: VistaInventario,
    meta: {
      requiereAutenticacion: true,
      roles: ['ADMIN', 'GESTOR']
    }
  },
  {
    path: '/perfil',
    name: 'DatosPersonales',
    component: VistaDatosPersonales,
    meta: {
      requiereAutenticacion: true,
      roles: ['ADMIN', 'CLIENTE'] // GESTOR no la ve según especificación
    }
  },
  {
    path: '/roles',
    name: 'GestionRoles',
    component: VistaGestionRoles,
    meta: {
      requiereAutenticacion: true,
      roles: ['ADMIN']
    }
  },
  {
    path: '/no-autorizado',
    name: 'NoAutorizado',
    component: VistaNoAutorizado
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guarda global de navegación por roles y autenticación
router.beforeEach((to, from, next) => {
  const autenticado = sesion.estaAutenticado.value
  const rol = sesion.rolActual.value

  // 1. Si la ruta es pública (ingreso o registro) y ya está logueado -> va al catálogo
  if (to.meta.publica && autenticado) {
    return next('/catalogo')
  }

  // 2. Si requiere autenticación y NO está logueado -> va a ingreso
  if (to.meta.requiereAutenticacion && !autenticado) {
    return next('/ingreso')
  }

  // 3. Si requiere roles específicos y el rol actual no está permitido -> no autorizado
  if (to.meta.roles && !to.meta.roles.includes(rol)) {
    return next('/no-autorizado')
  }

  next()
})

export default router
