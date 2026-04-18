<script setup lang="ts">
defineProps<{ modelValue: boolean; label: string; required?: boolean; }>();
defineEmits<{ (e: 'update:modelValue', v: boolean): void }>();
</script>

<template>
  <label class="flex items-start gap-3 mb-3 cursor-pointer">
    <input
      type="checkbox"
      :checked="modelValue"
      :required="required"
      @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
      class="mt-1 h-4 w-4 accent-fm-black"
    />
    <span class="text-sm">{{ label }}<span v-if="required" class="text-fm-red">*</span></span>
  </label>
</template>
