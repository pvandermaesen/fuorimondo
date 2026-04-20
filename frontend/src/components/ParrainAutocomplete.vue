<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue';
import { api } from '../api/client';
import type { ParrainOption } from '../api/types';

const props = defineProps<{
  modelValue: ParrainOption | null;
  label: string;
  placeholder?: string;
}>();
const emit = defineEmits<{ (e: 'update:modelValue', v: ParrainOption | null): void }>();

const query = ref('');
const results = ref<ParrainOption[]>([]);
const open = ref(false);
const loading = ref(false);
let debounceId: number | null = null;

watch(query, (q) => {
  if (debounceId !== null) window.clearTimeout(debounceId);
  debounceId = window.setTimeout(async () => {
    loading.value = true;
    try {
      results.value = await api.get<ParrainOption[]>(`/admin/users/parrains?q=${encodeURIComponent(q)}`);
      open.value = true;
    } finally {
      loading.value = false;
    }
  }, 250);
});

function pick(p: ParrainOption) {
  emit('update:modelValue', p);
  query.value = '';
  results.value = [];
  open.value = false;
}

function clear() {
  emit('update:modelValue', null);
}

function onBlur() {
  // delay to allow click on a result to register
  setTimeout(() => { open.value = false; }, 150);
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') open.value = false;
}

onBeforeUnmount(() => {
  if (debounceId !== null) window.clearTimeout(debounceId);
});
</script>

<template>
  <div class="mb-3">
    <label class="block text-sm mb-1">{{ label }}</label>
    <div v-if="modelValue" class="flex items-center justify-between border border-fm-black/20 rounded px-3 py-2 mb-2">
      <span class="text-sm">{{ modelValue.firstName }} {{ modelValue.lastName }} — {{ modelValue.email }}</span>
      <button type="button" class="text-xs underline text-fm-black/60 hover:text-fm-black" @click="clear" data-testid="parrain-clear">×</button>
    </div>
    <div class="relative">
      <input
        v-model="query"
        :placeholder="placeholder"
        class="w-full border border-fm-black/20 rounded px-3 py-2 text-sm"
        @focus="open = query.length > 0 || results.length > 0"
        @blur="onBlur"
        @keydown="onKey"
        data-testid="parrain-search"
      />
      <ul v-if="open && results.length > 0"
          class="absolute left-0 right-0 top-full mt-1 bg-white border border-fm-black/20 rounded shadow max-h-60 overflow-auto z-10"
          data-testid="parrain-results">
        <li v-for="p in results" :key="p.id"
            @mousedown.prevent="pick(p)"
            class="px-3 py-2 text-sm hover:bg-fm-black/5 cursor-pointer">
          {{ p.firstName }} {{ p.lastName }} <span class="text-fm-black/60">— {{ p.email }}</span>
        </li>
      </ul>
      <p v-else-if="open && !loading && query.length > 0" class="absolute left-0 right-0 top-full mt-1 bg-white border border-fm-black/20 rounded px-3 py-2 text-sm text-fm-black/60">
        —
      </p>
    </div>
  </div>
</template>
