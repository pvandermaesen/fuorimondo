<script setup lang="ts">
defineProps<{
  label?: string;
  modelValue: string | number | null | undefined;
  type?: string;
  required?: boolean;
  placeholder?: string;
  autocomplete?: string;
  error?: string;
  maxlength?: number | string;
}>();
defineEmits<{ (e: 'update:modelValue', v: string): void }>();
</script>

<template>
  <label class="block mb-4">
    <span v-if="label" class="block text-xs uppercase tracking-widest text-fm-black/70 mb-1">{{ label }}<span v-if="required" class="text-fm-red">*</span></span>
    <input
      :type="type || 'text'"
      :value="modelValue ?? ''"
      :placeholder="placeholder"
      :required="required"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      :class="[
        'block w-full border-0 border-b bg-transparent py-2 focus:outline-none focus:ring-0',
        error ? 'border-fm-red text-fm-red' : 'border-fm-black/30 focus:border-fm-black',
      ]"
    />
    <span v-if="error" class="text-xs text-fm-red mt-1 block">{{ error }}</span>
  </label>
</template>
