<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// Main application component
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* Fix for document preview dialog height */
.el-dialog__body {
  min-height: 70vh !important;
  height: 70vh !important;
}
</style>
