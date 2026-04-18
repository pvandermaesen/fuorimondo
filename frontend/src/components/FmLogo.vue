<script setup lang="ts">
defineProps<{ size?: 'sm' | 'md' | 'lg' }>();
</script>

<template>
  <div class="text-center select-none">
    <h1
      :class="[
        'font-logo tracking-[0.25em] text-fm-black',
        size === 'lg' ? 'text-3xl' : size === 'sm' ? 'text-lg' : 'text-2xl',
      ]"
    >FUORI MARMO</h1>
  </div>
</template>
