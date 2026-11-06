<script setup>
import BarraNavegacion from './componentes/BarraNavegacion.vue'
</script>

<template>
  <div class="aplicacion">
    <BarraNavegacion />
    <main class="contenido-principal">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.aplicacion {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.contenido-principal {
  flex: 1;
}
</style>
