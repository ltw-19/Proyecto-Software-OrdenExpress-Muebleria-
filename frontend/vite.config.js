import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Configuración de Vite para OrdenExpress Mueblería
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    open: true // Abre automáticamente el navegador al iniciar
  }
})