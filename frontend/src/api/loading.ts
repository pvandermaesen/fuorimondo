import { computed, ref } from 'vue';

export const inFlight = ref(0);
export const isLoading = computed(() => inFlight.value > 0);
