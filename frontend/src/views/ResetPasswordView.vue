<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import FmInput from '../components/FmInput.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const route = useRoute();

const tokenFromQuery = typeof route.query.token === 'string' ? route.query.token : '';

const email = ref('');
const password = ref('');
const submitted = ref(false);
const confirmed = ref(false);
const error = ref<string | null>(null);
const busy = ref(false);

async function requestReset() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/auth/password-reset/request', { email: email.value });
    submitted.value = true;
  } catch {
    error.value = t('common.error');
  } finally { busy.value = false; }
}

async function confirmReset() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/auth/password-reset/confirm', { token: tokenFromQuery, password: password.value });
    confirmed.value = true;
  } catch (err) {
    if (err instanceof ApiException && (err.status === 400 || err.status === 410)) error.value = t('reset.invalid');
    else error.value = t('common.error');
  } finally { busy.value = false; }
}
</script>

<template>
  <div class="fm-page max-w-sm">
    <template v-if="tokenFromQuery">
      <h2 class="text-xl mb-6 text-center">{{ t('reset.confirmTitle') }}</h2>
      <p v-if="confirmed" class="fm-legal-banner border-fm-black text-fm-black bg-fm-stone">{{ t('reset.confirmed') }}</p>
      <form v-else @submit.prevent="confirmReset">
        <FmInput v-model="password" :label="t('common.password')" type="password" required autocomplete="new-password" />
        <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
        <FmButton block type="submit" :disabled="busy">{{ t('reset.confirmSubmit') }}</FmButton>
      </form>
    </template>
    <template v-else>
      <h2 class="text-xl mb-6 text-center">{{ t('reset.title') }}</h2>
      <p v-if="submitted" class="fm-legal-banner border-fm-black text-fm-black bg-fm-stone">{{ t('reset.submitted') }}</p>
      <form v-else @submit.prevent="requestReset">
        <p class="text-sm text-fm-black/70 mb-5">{{ t('reset.enterEmail') }}</p>
        <FmInput v-model="email" :label="t('common.email')" type="email" required autocomplete="email" />
        <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
        <FmButton block type="submit" :disabled="busy">{{ t('reset.submit') }}</FmButton>
      </form>
    </template>
  </div>
</template>
