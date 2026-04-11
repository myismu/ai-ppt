<template>
  <router-view />
  <AiConsole />
</template>

<script lang="ts" setup>
import AiConsole from '@/components/AiConsole.vue'

if (import.meta.env.MODE !== 'development') {
  window.onbeforeunload = () => false
}
</script>

<style lang="scss">
#app {
  height: 100%;
}
</style>