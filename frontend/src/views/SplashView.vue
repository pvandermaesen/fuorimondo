<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/auth';
import FmLogo from '../components/FmLogo.vue';
import FmButton from '../components/FmButton.vue';
import taglineMark from '../assets/tagline-mark.png';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();

const homeContent = computed(() => {
  switch (auth.user?.status) {
    case 'WAITING_LIST':
      return { title: t('home.waitingListTitle'), message: t('home.waitingListMessage') };
    case 'ALLOCATAIRE':
      return { title: t('home.allocataireTitle'), message: t('home.allocataireMessage') };
    case 'SUSPENDED':
      return { title: t('home.suspendedTitle'), message: t('home.suspendedMessage') };
    default:
      return { title: t('home.waitingListTitle'), message: t('home.waitingListMessage') };
  }
});
</script>

<template>
  <div v-if="auth.isAuthenticated && auth.user"
       class="fm-page flex flex-col items-center justify-center min-h-[calc(100vh-7rem)] text-center space-y-8">
    <FmLogo size="lg" />
    <img :src="taglineMark" :alt="t('app.tagline')" class="w-40 sm:w-48 h-auto select-none pointer-events-none" />
    <p class="text-sm uppercase tracking-widest text-fm-black/60">
      {{ t('home.greeting', { name: auth.user.firstName }) }}
    </p>
    <div class="max-w-md space-y-4">
      <h2 class="font-serif text-2xl italic">{{ homeContent.title }}</h2>
      <p class="font-serif text-fm-black/80 leading-relaxed">{{ homeContent.message }}</p>
    </div>
    <div class="w-full max-w-xs">
      <FmButton block variant="ghost" @click="router.push({ name: 'profile' })" data-testid="home-goto-profile">
        {{ t('home.goProfile') }}
      </FmButton>
    </div>
  </div>

  <div v-else class="fm-page flex flex-col items-center justify-center min-h-[calc(100vh-7rem)] text-center space-y-10">
    <FmLogo size="lg" />
    <img :src="taglineMark" :alt="t('app.tagline')" class="w-40 sm:w-48 h-auto select-none pointer-events-none" />
    <p class="font-serif text-fm-black/80 leading-relaxed max-w-sm">{{ t('splash.concept') }}</p>
    <div class="w-full max-w-xs space-y-3">
      <FmButton block variant="primary" @click="router.push({ name: 'activate' })" data-testid="cta-activate">{{ t('splash.hasCode') }}</FmButton>
      <FmButton block variant="secondary" @click="router.push({ name: 'register' })" data-testid="cta-register">{{ t('splash.joinWaitingList') }}</FmButton>
    </div>
    <div class="w-full max-w-xs pt-4 border-t border-fm-black/10 space-y-2">
      <p class="text-sm text-fm-black/60">{{ t('splash.alreadyMember') }}</p>
      <FmButton block variant="ghost" @click="router.push({ name: 'login' })" data-testid="cta-login">{{ t('splash.login') }}</FmButton>
    </div>
  </div>
</template>
