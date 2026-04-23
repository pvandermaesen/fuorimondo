<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse } from '../api/types';
import OrderStatusBadge from '../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const order = ref<OrderResponse | null>(null);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

onMounted(async () => {
  order.value = await api.get<OrderResponse>(`/orders/${route.params.id}`);
});
</script>

<template>
  <div class="fm-page max-w-lg" v-if="order">
    <h2 class="text-2xl mb-1 font-serif italic">{{ order.product.name }}</h2>
    <div class="mb-6"><OrderStatusBadge :status="order.status" /></div>
    <dl class="text-sm space-y-2">
      <div><dt class="text-fm-black/60">{{ t('order.orderId') }}</dt><dd>{{ order.id }}</dd></div>
      <div><dt class="text-fm-black/60">Total</dt><dd class="font-logo">{{ fmtPrice(order.totalEur) }}</dd></div>
      <div v-if="order.paidAt"><dt class="text-fm-black/60">{{ t('order.paidAt') }}</dt><dd>{{ fmtDate(order.paidAt) }}</dd></div>
      <div v-if="order.shippingAddress">
        <dt class="text-fm-black/60">{{ t('checkout.shippingAddress') }}</dt>
        <dd>
          {{ order.shippingAddress.fullName }}<br />
          {{ order.shippingAddress.street }}<br />
          {{ order.shippingAddress.postalCode }} {{ order.shippingAddress.city }}<br />
          {{ order.shippingAddress.country }}
        </dd>
      </div>
    </dl>
  </div>
</template>
