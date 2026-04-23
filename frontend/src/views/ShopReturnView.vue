<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const order = ref<OrderResponse | null>(null);
const attempts = ref(0);
const timer = ref<number | null>(null);
const isDevSim = computed(() => route.query.sim === '1');

async function fetchOnce() {
  order.value = await api.get<OrderResponse>(`/orders/${route.params.id}`);
}

function startPolling() {
  if (timer.value) return;
  timer.value = window.setInterval(async () => {
    attempts.value++;
    await fetchOnce();
    if (order.value && order.value.status !== 'PENDING_PAYMENT') stopPolling();
    if (attempts.value >= 15) stopPolling();
  }, 2000);
}

function stopPolling() {
  if (timer.value) { clearInterval(timer.value); timer.value = null; }
}

async function simulate(status: 'paid' | 'failed' | 'cancelled' | 'expired') {
  await api.post(`/dev/orders/${route.params.id}/simulate-webhook?status=${status}`);
  await fetchOnce();
}

function retry() {
  if (order.value) router.push({ name: 'shop-product', params: { id: order.value.product.id } });
}

onMounted(async () => {
  await fetchOnce();
  if (order.value?.status === 'PENDING_PAYMENT' && !isDevSim.value) startPolling();
});

onUnmounted(stopPolling);
</script>

<template>
  <div class="fm-page max-w-lg text-center space-y-6">
    <template v-if="order">
      <div v-if="order.status === 'PAID'">
        <h2 class="text-3xl font-serif italic text-green-800">{{ t('orderReturn.paid') }}</h2>
        <p class="text-sm text-fm-black/60 mt-2">{{ t('orderReturn.paidMessage') }}</p>
        <div class="pt-6 space-y-3">
          <FmButton block variant="primary" @click="router.push({ name: 'order-detail', params: { id: order.id } })">{{ t('orderReturn.viewOrder') }}</FmButton>
          <FmButton block variant="ghost" @click="router.push({ name: 'shop' })">{{ t('orderReturn.backToShop') }}</FmButton>
        </div>
      </div>
      <div v-else-if="order.status === 'FAILED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.failed') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else-if="order.status === 'CANCELLED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.cancelled') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else-if="order.status === 'EXPIRED'">
        <h2 class="text-3xl font-serif italic text-fm-red">{{ t('orderReturn.expired') }}</h2>
        <FmButton block variant="primary" @click="retry">{{ t('orderReturn.retry') }}</FmButton>
      </div>
      <div v-else>
        <h2 class="text-xl font-serif italic">{{ t('common.loading') }}</h2>
        <p v-if="attempts >= 15" class="text-sm text-fm-black/60 mt-4">{{ t('orderReturn.timeout') }}</p>
      </div>

      <section v-if="isDevSim && order.status === 'PENDING_PAYMENT'" class="fm-card border-dashed border-fm-gold text-left space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('orderReturn.simulateTitle') }}</p>
        <div class="flex gap-2 flex-wrap">
          <FmButton variant="primary" @click="simulate('paid')" data-testid="sim-paid">paid</FmButton>
          <FmButton variant="secondary" @click="simulate('failed')">failed</FmButton>
          <FmButton variant="ghost" @click="simulate('cancelled')">cancelled</FmButton>
          <FmButton variant="ghost" @click="simulate('expired')">expired</FmButton>
        </div>
      </section>
    </template>
    <p v-else>{{ t('common.loading') }}</p>
  </div>
</template>
