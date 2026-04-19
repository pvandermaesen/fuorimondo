<script setup lang="ts">
defineProps<{ size?: 'sm' | 'md' | 'lg' }>();
</script>

<template>
  <div class="text-center select-none">
    <router-link
      to="/"
      :class="[
        'font-logo tracking-[0.25em] text-fm-black block',
        size === 'lg' ? 'text-3xl' : size === 'sm' ? 'text-lg' : 'text-2xl',
      ]"
      aria-label="Fuori Marmo"
    >FUORI MARMO</router-link>
  </div>
</template>
