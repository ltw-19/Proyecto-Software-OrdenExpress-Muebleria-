<script setup>
</script>

<template>
  <div class="contenedor">
    <div class="tarjeta tarjeta-aviso">
      <h1>Acceso Denegado (403)</h1>
      <p>No tienes los permisos requeridos para ingresar a esta sección.</p>
      <router-link to="/catalogo" class="btn btn-primario">
        Volver al Catálogo
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.tarjeta-aviso {
  max-width: 500px;
  margin: 3rem auto;
  text-align: center;
}
</style>
