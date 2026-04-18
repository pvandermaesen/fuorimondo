<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import type { RegisterWaitingListRequest, Locale } from '../api/types';
import FmInput from '../components/FmInput.vue';
import FmSelect from '../components/FmSelect.vue';
import FmCheckbox from '../components/FmCheckbox.vue';
import FmButton from '../components/FmButton.vue';
import { useAuthStore } from '../stores/auth';

const { t, locale } = useI18n();
const router = useRouter();
const auth = useAuthStore();

const form = reactive<RegisterWaitingListRequest>({
  email: '', firstName: '', lastName: '', phone: '',
  country: '', city: '', referrerInfo: '',
  locale: (locale.value.toUpperCase() as Locale),
  password: '', acceptTerms: false, acceptPrivacy: false,
});

const error = ref<string | null>(null);
const success = ref(false);
const busy = ref(false);

const localeOptions = [
  { value: 'FR', label: 'Français' },
  { value: 'IT', label: 'Italiano' },
  { value: 'EN', label: 'English' },
];

async function submit() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/auth/register', form);
    await auth.login({ email: form.email, password: form.password });
    success.value = true;
    setTimeout(() => router.push({ name: 'profile' }), 1500);
  } catch (err) {
    if (err instanceof ApiException && err.status === 409) error.value = t('register.emailExists');
    else if (err instanceof ApiException && err.status === 400) error.value = t('common.error');
    else error.value = t('common.error');
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="fm-page">
    <h2 class="text-xl mb-6 text-center">{{ t('register.title') }}</h2>
    <p v-if="success" class="fm-legal-banner border-fm-black text-fm-black bg-fm-stone">{{ t('register.success') }}</p>
    <form v-else @submit.prevent="submit">
      <FmInput v-model="form.email" :label="t('common.email')" type="email" required autocomplete="email" data-testid="reg-email" />
      <FmInput v-model="form.firstName" :label="t('common.firstName')" required autocomplete="given-name" data-testid="reg-first" />
      <FmInput v-model="form.lastName" :label="t('common.lastName')" required autocomplete="family-name" data-testid="reg-last" />
      <FmInput v-model="form.phone" :label="t('common.phone')" autocomplete="tel" />
      <FmInput v-model="form.country" :label="t('common.country')" required autocomplete="country-name" />
      <FmInput v-model="form.city" :label="t('common.city')" required autocomplete="address-level2" />
      <FmInput v-model="form.referrerInfo" :label="t('register.referrer')" :maxlength="2000" />
      <FmSelect v-model="form.locale" :label="t('common.locale')" :options="localeOptions" required />
      <FmInput v-model="form.password" :label="t('common.password')" type="password" required autocomplete="new-password" data-testid="reg-password" />
      <FmCheckbox v-model="form.acceptTerms" :label="t('register.acceptTerms')" required />
      <FmCheckbox v-model="form.acceptPrivacy" :label="t('register.acceptPrivacy')" required />
      <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
      <FmButton block type="submit" :disabled="busy" data-testid="reg-submit">{{ t('register.submit') }}</FmButton>
    </form>
  </div>
</template>
