<script setup lang="ts">
defineProps<{
  label?: string;
  modelValue: string | null | undefined;
  options: Array<{ value: string; label: string }>;
  required?: boolean;
  error?: string;
}>();
defineEmits<{ (e: 'update:modelValue', v: string): void }>();
</script>

<template>
  <label class="block mb-4">
    <span v-if="label" class="block text-xs uppercase tracking-widest text-fm-black/70 mb-1">{{ label }}<span v-if="required" class="text-fm-red">*</span></span>
    <select
      :value="modelValue ?? ''"
      :required="required"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
      :class="[
        'block w-full border-0 border-b bg-transparent py-2 focus:outline-none focus:ring-0',
        error ? 'border-fm-red text-fm-red' : 'border-fm-black/30 focus:border-fm-black',
      ]"
    >
      <option value="" disabled></option>
      <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
    </select>
    <span v-if="error" class="text-xs text-fm-red mt-1 block">{{ error }}</span>
  </label>
</template>
