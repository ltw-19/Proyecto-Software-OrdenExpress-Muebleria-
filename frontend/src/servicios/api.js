import axios from 'axios'
import { sesion } from '../estado/sesion.js'

// Instancia única de Axios para todo el proyecto
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Interceptor de solicitudes: adjunta el token JWT si existe
api.interceptors.request.use(
  (config) => {
    const token = sesion.obtenerToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Interceptor de respuestas: maneja 401 (sesión inválida o expirada)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Limpia la sesión y redirige al ingreso
      sesion.cerrarSesion()
      if (window.location.pathname !== '/ingreso') {
        window.location.href = '/ingreso'
      }
    }
    return Promise.reject(error)
  }
)

export default api
