<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { OrderStatus } from '../api/types';

const props = defineProps<{ status: OrderStatus }>();
const { t } = useI18n();

const classes = computed(() => {
  switch (props.status) {
    case 'PAID': return 'bg-green-100 text-green-800 border-green-300';
    case 'PENDING_PAYMENT': return 'bg-fm-stone text-fm-black/70 border-fm-black/20';
    case 'FAILED': return 'bg-red-100 text-fm-red border-fm-red/40';
    case 'CANCELLED': return 'bg-fm-stone text-fm-red border-fm-red/30';
    case 'EXPIRED': return 'bg-fm-stone text-fm-black/40 border-fm-black/20';
  }
});

const label = computed(() => {
  switch (props.status) {
    case 'PAID': return t('order.statusPaid');
    case 'PENDING_PAYMENT': return t('order.statusPendingPayment');
    case 'FAILED': return t('order.statusFailed');
    case 'CANCELLED': return t('order.statusCancelled');
    case 'EXPIRED': return t('order.statusExpired');
  }
});
</script>

<template>
  <span :class="['inline-block px-2 py-0.5 text-xs uppercase tracking-widest border rounded', classes]">
    {{ label }}
  </span>
</template>
