<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/auth';
import FmLogo from '../components/FmLogo.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
  if (auth.isAuthenticated) router.replace({ name: 'profile' });
});
</script>

<template>
  <div class="fm-page flex flex-col items-center justify-center min-h-[calc(100vh-7rem)] text-center space-y-10">
    <FmLogo size="lg" />
    <p class="font-serif italic text-fm-black/70">{{ t('app.tagline') }}</p>
    <div class="w-full max-w-xs space-y-3">
      <FmButton block variant="primary" @click="router.push({ name: 'activate' })" data-testid="cta-activate">{{ t('splash.hasCode') }}</FmButton>
      <FmButton block variant="secondary" @click="router.push({ name: 'register' })" data-testid="cta-register">{{ t('splash.joinWaitingList') }}</FmButton>
      <FmButton block variant="ghost" @click="router.push({ name: 'login' })" data-testid="cta-login">{{ t('splash.login') }}</FmButton>
    </div>
  </div>
</template>
