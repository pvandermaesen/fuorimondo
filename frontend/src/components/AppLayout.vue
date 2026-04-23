<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/auth';
import { setLocale } from '../i18n';
import FmLogo from './FmLogo.vue';
import { isLoading } from '../api/loading';

const open = ref(false);
const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();
const auth = useAuthStore();

watch(() => route.fullPath, () => (open.value = false));

async function logout() {
  await auth.logout();
  open.value = false;
  router.push({ name: 'splash' });
}

function changeLocale(l: 'fr' | 'it' | 'en') {
  setLocale(l);
  locale.value = l;
}
</script>

<template>
  <div class="min-h-screen bg-fm-white flex flex-col desk:flex-row">
    <!-- Mobile header with burger -->
    <header class="w-full border-b border-fm-black/10 desk:hidden">
      <div class="mx-auto max-w-mobile px-5 h-14 flex items-center justify-between">
        <button
          class="p-2 -ml-2"
          :aria-label="t('nav.menu')"
          @click="open = true"
          data-testid="burger"
        >
          <svg v-if="isLoading" class="animate-spin" width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" stroke-opacity="0.25" />
            <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M3 6h18M3 12h18M3 18h18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </button>
        <FmLogo size="sm" />
        <div class="w-8" aria-hidden="true" />
      </div>
    </header>

    <!-- Overlay (mobile only, when drawer open) -->
    <div
      v-if="open"
      class="fixed inset-0 z-40 bg-fm-black/40 desk:hidden"
      @click="open = false"
    />

    <!-- Sidebar: slide-in drawer on mobile, fixed column on desk -->
    <aside
      class="fixed inset-y-0 left-0 z-50 w-80 max-w-[85vw] bg-fm-white shadow-xl flex flex-col
             transform transition-transform duration-200
             desk:static desk:translate-x-0 desk:w-64 desk:max-w-none desk:shadow-none
             desk:border-r desk:border-fm-black/10 desk:z-auto desk:h-screen desk:sticky desk:top-0"
      :class="open ? 'translate-x-0' : '-translate-x-full desk:translate-x-0'"
      role="navigation"
      :aria-label="t('nav.menu')"
    >
      <div class="h-14 px-5 flex items-center justify-between border-b border-fm-black/10">
        <FmLogo size="sm" />
        <button class="p-2 -mr-2 desk:hidden" :aria-label="t('nav.close')" @click="open = false">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </button>
      </div>
      <nav class="flex-1 overflow-y-auto px-5 py-4 space-y-1">
        <template v-if="auth.isAuthenticated">
          <router-link to="/" class="block py-2 text-base">{{ t('nav.home') }}</router-link>
          <router-link v-if="auth.isAllocataire" to="/shop" class="block py-2 text-base">{{ t('nav.shop') }}</router-link>
          <router-link to="/orders" class="block py-2 text-base">{{ t('nav.myOrders') }}</router-link>
          <router-link to="/profile" class="block py-2 text-base">{{ t('nav.profile') }}</router-link>
          <router-link v-if="auth.isAllocataire" to="/addresses" class="block py-2 text-base">{{ t('nav.addresses') }}</router-link>
          <router-link to="/settings" class="block py-2 text-base">{{ t('nav.settings') }}</router-link>
          <div v-if="auth.isAdmin" class="pt-3 mt-3 border-t border-fm-black/10">
            <div class="text-xs uppercase tracking-widest text-fm-black/50 py-2">{{ t('nav.admin') }}</div>
            <router-link to="/admin/users" class="block py-2 text-base">{{ t('nav.adminUsers') }}</router-link>
            <router-link to="/admin/products" class="block py-2 text-base">{{ t('nav.adminProducts') }}</router-link>
            <router-link to="/admin/orders" class="block py-2 text-base">{{ t('nav.adminOrders') }}</router-link>
          </div>
        </template>
        <template v-else>
          <router-link to="/login" class="block py-2 text-base">{{ t('nav.login') }}</router-link>
        </template>
        <div class="pt-3 mt-3 border-t border-fm-black/10">
          <div class="text-xs uppercase tracking-widest text-fm-black/50 py-2">{{ t('nav.legal') }}</div>
          <router-link :to="{ name: 'legal', params: { slug: 'cgu' } }" class="block py-2 text-base">{{ t('legal.cgu') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'cgv' } }" class="block py-2 text-base">{{ t('legal.cgv') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'privacy' } }" class="block py-2 text-base">{{ t('legal.privacy') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'cookies' } }" class="block py-2 text-base">{{ t('legal.cookies') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'mentions' } }" class="block py-2 text-base">{{ t('legal.mentions') }}</router-link>
        </div>
      </nav>
      <div class="border-t border-fm-black/10 px-5 py-4 space-y-3">
        <div class="flex gap-2 text-xs uppercase tracking-widest">
          <button
            v-for="l in (['fr','it','en'] as const)" :key="l"
            :class="['px-2 py-1 border', locale === l ? 'border-fm-black bg-fm-black text-fm-white' : 'border-fm-black/20']"
            @click="changeLocale(l)"
          >{{ l }}</button>
        </div>
        <button
          v-if="auth.isAuthenticated"
          class="block w-full text-left py-2 text-sm uppercase tracking-widest text-fm-red"
          @click="logout"
          data-testid="logout"
        >{{ t('nav.logout') }}</button>
      </div>
    </aside>

    <div class="flex-1 flex flex-col min-w-0">
      <main class="flex-1">
        <slot />
      </main>

      <footer class="border-t border-fm-black/10 text-xs text-fm-black/50 py-4">
        <div class="mx-auto max-w-mobile px-5 flex gap-4 flex-wrap justify-center">
          <router-link :to="{ name: 'legal', params: { slug: 'cgu' } }">{{ t('legal.cgu') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'privacy' } }">{{ t('legal.privacy') }}</router-link>
          <router-link :to="{ name: 'legal', params: { slug: 'mentions' } }">{{ t('legal.mentions') }}</router-link>
        </div>
      </footer>
    </div>
  </div>
</template>
