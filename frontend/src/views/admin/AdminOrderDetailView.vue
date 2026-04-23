<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { AdminOrderResponse } from '../../api/types';
import OrderStatusBadge from '../../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const order = ref<AdminOrderResponse | null>(null);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

onMounted(async () => {
  order.value = await api.get<AdminOrderResponse>(`/admin/orders/${route.params.id}`);
});
</script>

<template>
  <div class="fm-page max-w-2xl" v-if="order">
    <h2 class="text-2xl mb-1 font-serif italic">{{ order.product.name }}</h2>
    <div class="mb-6 flex items-center gap-4">
      <OrderStatusBadge :status="order.status" />
      <span class="text-sm text-fm-black/60">{{ fmtDate(order.createdAt) }}</span>
    </div>
    <dl class="text-sm space-y-2">
      <div><dt class="text-fm-black/60">{{ t('order.orderId') }}</dt><dd>{{ order.id }}</dd></div>
      <div>
        <dt class="text-fm-black/60">Acheteur</dt>
        <dd>
          <router-link :to="{ name: 'admin-user-detail', params: { id: order.userId } }" class="underline">
            {{ order.userFirstName }} {{ order.userLastName }}
          </router-link>
          — {{ order.userEmail }}
        </dd>
      </div>
      <div><dt class="text-fm-black/60">Total</dt><dd class="font-logo">{{ fmtPrice(order.totalEur) }}</dd></div>
      <div v-if="order.paidAt"><dt class="text-fm-black/60">{{ t('order.paidAt') }}</dt><dd>{{ fmtDate(order.paidAt) }}</dd></div>
      <div v-if="order.molliePaymentId"><dt class="text-fm-black/60">{{ t('order.mollieRef') }}</dt><dd>{{ order.molliePaymentId }}</dd></div>
      <div v-if="order.shipping">
        <dt class="text-fm-black/60">{{ t('checkout.shippingAddress') }}</dt>
        <dd>
          {{ order.shipping.fullName }}<br />
          {{ order.shipping.street }}<br />
          {{ order.shipping.postalCode }} {{ order.shipping.city }}<br />
          {{ order.shipping.country }}
        </dd>
      </div>
    </dl>
  </div>
</template>
