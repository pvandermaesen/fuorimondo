<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import type { PublicProductResponse, AddressResponse, CreateOrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';
import FmSelect from '../components/FmSelect.vue';
import FmCheckbox from '../components/FmCheckbox.vue';

const { t, locale } = useI18n();
const route = useRoute();

const product = ref<PublicProductResponse | null>(null);
const addresses = ref<AddressResponse[]>([]);
const selectedAddressId = ref<string>('');
const acceptCgv = ref(false);
const busy = ref(false);
const error = ref<string | null>(null);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}

const addressOptions = computed(() =>
  addresses.value.map(a => ({ value: a.id, label: `${a.fullName} — ${a.street}, ${a.postalCode} ${a.city}` }))
);

const canPay = computed(() => {
  if (!acceptCgv.value || !product.value) return false;
  if (product.value.delivery && !selectedAddressId.value) return false;
  return true;
});

async function load() {
  loading.value = true;
  try {
    product.value = await api.get<PublicProductResponse>(`/products/${route.params.productId}`);
    if (product.value.delivery) {
      addresses.value = await api.get<AddressResponse[]>('/me/addresses?type=SHIPPING');
      const defaultOne = addresses.value.find(a => a.isDefault);
      selectedAddressId.value = (defaultOne ?? addresses.value[0])?.id ?? '';
    }
  } finally {
    loading.value = false;
  }
}

async function pay() {
  if (!product.value) return;
  busy.value = true; error.value = null;
  try {
    const res = await api.post<CreateOrderResponse>('/orders', {
      productId: product.value.id,
      shippingAddressId: product.value.delivery ? selectedAddressId.value : null,
    });
    window.location.href = res.checkoutUrl;
  } catch (e) {
    if (e instanceof ApiException) {
      switch (e.payload?.code) {
        case 'out_of_stock': error.value = t('errors.outOfStock'); break;
        case 'sale_window_closed': error.value = t('errors.saleWindowClosed'); break;
        case 'tier_mismatch': error.value = t('errors.tierMismatch'); break;
        case 'no_shipping_address': error.value = t('errors.noShippingAddress'); break;
        default: error.value = t('errors.paymentError');
      }
    } else {
      error.value = t('errors.paymentError');
    }
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('checkout.title') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <div v-else-if="product" class="space-y-6">
      <section class="fm-card space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('checkout.summary') }}</p>
        <p class="font-serif italic text-lg">{{ product.name }}</p>
        <p class="font-logo text-xl">{{ fmtPrice(product.priceEur) }}</p>
      </section>

      <section v-if="product.delivery" class="space-y-2">
        <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('checkout.shippingAddress') }}</p>
        <template v-if="addresses.length > 0">
          <FmSelect v-model="selectedAddressId" :options="addressOptions" data-testid="address-select" />
        </template>
        <div v-else class="fm-card bg-fm-stone text-sm space-y-2">
          <p>{{ t('checkout.noShippingAddress') }}</p>
          <router-link to="/addresses" class="underline">{{ t('checkout.addAddressLink') }}</router-link>
        </div>
      </section>

      <FmCheckbox v-model="acceptCgv" :label="t('checkout.acceptCgv')" data-testid="accept-cgv" />
      <router-link :to="{ name: 'legal', params: { slug: 'cgv' } }" class="text-xs underline">{{ t('legal.cgv') }}</router-link>

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>

      <FmButton block variant="primary" :disabled="!canPay || busy" @click="pay" data-testid="pay-cta">
        {{ t('checkout.payCta', { price: fmtPrice(product.priceEur) }) }}
      </FmButton>
    </div>
  </div>
</template>
