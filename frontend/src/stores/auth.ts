import { defineStore } from 'pinia';
import { api, ApiException } from '../api/client';
import type { UserResponse, LoginRequest } from '../api/types';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  loaded: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ user: null, loading: false, loaded: false }),
  getters: {
    isAuthenticated: (s) => s.user !== null,
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isAllocataire: (s) => s.user?.status === 'ALLOCATAIRE',
    isWaitingList: (s) => s.user?.status === 'WAITING_LIST',
  },
  actions: {
    async fetchMe(): Promise<UserResponse | null> {
      this.loading = true;
      try {
        this.user = await api.get<UserResponse>('/me');
        return this.user;
      } catch (err) {
        if (err instanceof ApiException && (err.status === 401 || err.status === 403)) {
          this.user = null;
          return null;
        }
        throw err;
      } finally {
        this.loading = false;
        this.loaded = true;
      }
    },

    async login(payload: LoginRequest) {
      await api.post<void>('/auth/login', payload);
      await this.fetchMe();
    },

    async logout() {
      try { await api.post<void>('/auth/logout'); } catch { /* session may already be dead */ }
      this.user = null;
    },

    setUser(u: UserResponse | null) { this.user = u; },
  },
});
