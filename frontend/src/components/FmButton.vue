<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  type?: 'button' | 'submit';
  disabled?: boolean;
  block?: boolean;
}>(), { variant: 'primary', type: 'button', disabled: false, block: false });
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="[
      'inline-flex items-center justify-center px-5 py-3 text-sm tracking-wide uppercase transition',
      block && 'w-full',
      variant === 'primary' && 'bg-fm-black text-fm-white hover:bg-fm-black/90 disabled:opacity-50',
      variant === 'secondary' && 'bg-fm-white text-fm-black border border-fm-black hover:bg-fm-stone disabled:opacity-50',
      variant === 'ghost' && 'bg-transparent text-fm-black hover:bg-fm-stone disabled:opacity-50',
      variant === 'danger' && 'bg-fm-red text-fm-white hover:bg-fm-red/90 disabled:opacity-50',
      disabled && 'cursor-not-allowed',
    ]"
  >
    <slot />
  </button>
</template>
