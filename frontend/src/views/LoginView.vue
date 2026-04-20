<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/auth';
import { ApiException } from '../api/client';
import FmInput from '../components/FmInput.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const form = reactive({ email: '', password: '' });
const error = ref<string | null>(null);
const busy = ref(false);

async function submit() {
  error.value = null;
  busy.value = true;
  try {
    await auth.login({ email: form.email, password: form.password });
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile';
    router.push(redirect);
  } catch (err) {
    if (err instanceof ApiException && (err.status === 401 || err.status === 403)) {
      error.value = t('login.invalid');
    } else {
      error.value = t('common.error');
    }
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="fm-page max-w-xs">
    <h2 class="text-xl mb-6 text-center">{{ t('login.title') }}</h2>
    <form @submit.prevent="submit" method="post" action="/api/auth/login" name="login">
      <FmInput v-model="form.email" :label="t('common.email')" type="email" required autocomplete="username" name="email" id="email" />
      <FmInput v-model="form.password" :label="t('common.password')" type="password" required autocomplete="current-password" name="password" id="password" />
      <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
      <FmButton block type="submit" :disabled="busy" data-testid="login-submit">{{ t('login.submit') }}</FmButton>
    </form>
    <div class="text-center mt-6 text-sm">
      <router-link class="underline" to="/reset-password">{{ t('login.forgot') }}</router-link>
    </div>
  </div>
</template>
