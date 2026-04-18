<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import FmInput from '../components/FmInput.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();

const form = reactive({ currentPassword: '', newPassword: '' });
const error = ref<string | null>(null);
const done = ref(false);
const busy = ref(false);

async function submit() {
  error.value = null;
  busy.value = true;
  try {
    await api.post('/me/password', form);
    done.value = true;
    form.currentPassword = '';
    form.newPassword = '';
  } catch (err) {
    if (err instanceof ApiException && err.status === 400) error.value = t('settings.invalidCurrent');
    else error.value = t('common.error');
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="fm-page max-w-sm">
    <h2 class="text-2xl mb-6">{{ t('settings.title') }}</h2>
    <section class="fm-card">
      <h3 class="text-lg mb-4">{{ t('settings.changePassword') }}</h3>
      <p v-if="done" class="fm-legal-banner border-fm-black bg-fm-stone text-fm-black">{{ t('settings.changed') }}</p>
      <form @submit.prevent="submit">
        <FmInput v-model="form.currentPassword" :label="t('settings.currentPassword')" type="password" required autocomplete="current-password" />
        <FmInput v-model="form.newPassword" :label="t('settings.newPassword')" type="password" required autocomplete="new-password" />
        <p v-if="error" class="text-sm text-fm-red mb-3">{{ error }}</p>
        <FmButton block type="submit" :disabled="busy">{{ t('common.save') }}</FmButton>
      </form>
    </section>
  </div>
</template>
