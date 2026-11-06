<script setup>
defineProps({
  tipo: {
    type: String,
    default: 'error', // 'error', 'exito', 'aviso'
    validator: (v) => ['error', 'exito', 'aviso'].includes(v)
  },
  mensaje: {
    type: String,
    required: true
  }
})

defineEmits(['cerrar'])
</script>

<template>
  <div v-if="mensaje" :class="['alerta', `alerta-${tipo}`]" role="alert">
    <div class="contenido-alerta">
      <span class="icono-alerta" aria-hidden="true">
        <template v-if="tipo === 'error'">⚠️</template>
        <template v-else-if="tipo === 'exito'">✓</template>
        <template v-else>ℹ️</template>
      </span>
      <span class="texto-mensaje">{{ mensaje }}</span>
    </div>
    <button type="button" class="btn-cerrar" aria-label="Cerrar alerta" @click="$emit('cerrar')">
      &times;
    </button>
  </div>
</template>

<style scoped>
.contenido-alerta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex: 1;
}

.icono-alerta {
  font-weight: bold;
}

.texto-mensaje {
  word-break: break-word;
}

.btn-cerrar {
  background: transparent;
  border: none;
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
  color: inherit;
  opacity: 0.7;
  padding: 0 0.25rem;
}

.btn-cerrar:hover {
  opacity: 1;
}
</style>
