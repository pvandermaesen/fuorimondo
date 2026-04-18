<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import { useAuthStore } from '../stores/auth';
import FmInput from '../components/FmInput.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();

const step = ref<1 | 2 | 3>(1);
const form = reactive({ email: '', code: '', password: '' });
const error = ref<string | null>(null);
const busy = ref(false);

async function verify() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/auth/activate/verify', { email: form.email, code: form.code.toUpperCase() });
    step.value = 2;
  } catch (err) {
    if (err instanceof ApiException) error.value = t('activate.invalid');
    else error.value = t('common.error');
  } finally { busy.value = false; }
}

async function setPassword() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/auth/activate', { email: form.email, code: form.code.toUpperCase(), password: form.password });
    await auth.login({ email: form.email, password: form.password });
    step.value = 3;
  } catch (err) {
    error.value = t('common.error');
  } finally { busy.value = false; }
}
</script>

<template>
  <div class="fm-page max-w-sm">
    <h2 class="text-xl mb-6 text-center">{{ t('activate.title') }}</h2>

    <div v-if="step === 1">
      <p class="text-sm text-fm-black/70 mb-5">{{ t('activate.step1') }}</p>
      <form @submit.prevent="verify">
        <FmInput v-model="form.email" :label="t('common.email')" type="email" required autocomplete="email" data-testid="act-email" />
        <FmInput v-model="form.code" :label="t('activate.code')" required :maxlength="6" data-testid="act-code" />
        <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
        <FmButton block type="submit" :disabled="busy" data-testid="act-verify">{{ t('activate.verifyCode') }}</FmButton>
      </form>
    </div>

    <div v-else-if="step === 2">
      <p class="text-sm text-fm-black/70 mb-5">{{ t('activate.step2') }}</p>
      <form @submit.prevent="setPassword">
        <FmInput v-model="form.password" :label="t('activate.newPassword')" type="password" required autocomplete="new-password" data-testid="act-password" />
        <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
        <FmButton block type="submit" :disabled="busy" data-testid="act-submit">{{ t('common.continue') }}</FmButton>
      </form>
    </div>

    <div v-else class="text-center">
      <p class="text-sm font-serif italic mb-6">{{ t('activate.step3') }}</p>
      <p class="text-sm mb-6">{{ t('activate.welcome') }}</p>
      <FmButton block @click="router.push({ name: 'profile' })" data-testid="act-goto-profile">{{ t('activate.goProfile') }}</FmButton>
    </div>
  </div>
</template>
