<script setup lang="ts">
defineProps<{ modelValue: boolean; label: string; disabled?: boolean; }>();
defineEmits<{ (e: 'update:modelValue', v: boolean): void }>();
</script>

<template>
  <label class="flex items-center gap-3 mb-3 cursor-pointer" :class="{ 'opacity-50 pointer-events-none': disabled }">
    <span class="relative inline-block w-10 h-6">
      <input
        type="checkbox"
        :checked="modelValue"
        :disabled="disabled"
        @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
        class="peer sr-only"
      />
      <span class="absolute inset-0 rounded-full bg-fm-black/20 transition peer-checked:bg-fm-black"></span>
      <span class="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white transition peer-checked:translate-x-4"></span>
    </span>
    <span class="text-sm">{{ label }}</span>
  </label>
</template>
