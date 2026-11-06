<script setup>
import { sesion } from '../estado/sesion.js'
</script>

<template>
  <div class="contenedor">
    <div class="tarjeta">
      <h1>Catálogo de Muebles</h1>
      <p>Bienvenido, {{ sesion.nombreCompleto.value || 'Usuario' }}.</p>
      <p class="texto-ayuda">
        Esta sección corresponde al módulo de Catálogo de Muebles.
      </p>
    </div>
  </div>
</template>
