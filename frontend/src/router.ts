import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from './stores/auth';

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'splash', component: () => import('./views/SplashView.vue'), meta: { public: true } },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('./views/RegisterView.vue'), meta: { public: true } },
  { path: '/activate', name: 'activate', component: () => import('./views/ActivateView.vue'), meta: { public: true } },
  { path: '/reset-password', name: 'reset-password', component: () => import('./views/ResetPasswordView.vue'), meta: { public: true } },
  { path: '/profile', name: 'profile', component: () => import('./views/ProfileView.vue') },
  { path: '/addresses', name: 'addresses', component: () => import('./views/AddressesView.vue') },
  { path: '/settings', name: 'settings', component: () => import('./views/SettingsView.vue') },
  { path: '/legal/:slug', name: 'legal', component: () => import('./views/LegalView.vue'), meta: { public: true } },
  { path: '/admin/users', name: 'admin-users', component: () => import('./views/admin/AdminUsersView.vue'), meta: { admin: true } },
  { path: '/admin/users/create', name: 'admin-users-create', component: () => import('./views/admin/AdminCreateUserView.vue'), meta: { admin: true } },
  { path: '/admin/users/:id', name: 'admin-user-detail', component: () => import('./views/admin/AdminUserDetailView.vue'), meta: { admin: true } },
  { path: '/admin/products', name: 'admin-products', component: () => import('./views/admin/AdminProductsView.vue'), meta: { admin: true } },
  { path: '/admin/products/create', name: 'admin-products-create', component: () => import('./views/admin/AdminCreateProductView.vue'), meta: { admin: true } },
  { path: '/admin/products/:id', name: 'admin-product-detail', component: () => import('./views/admin/AdminProductDetailView.vue'), meta: { admin: true } },
  { path: '/shop', name: 'shop', component: () => import('./views/ShopView.vue') },
  { path: '/shop/:id', name: 'shop-product', component: () => import('./views/ShopProductView.vue') },
  { path: '/shop/checkout/:productId', name: 'shop-checkout', component: () => import('./views/ShopCheckoutView.vue') },
  { path: '/shop/order/:id/return', name: 'shop-return', component: () => import('./views/ShopReturnView.vue') },
  { path: '/orders', name: 'my-orders', component: () => import('./views/MyOrdersView.vue') },
  { path: '/orders/:id', name: 'order-detail', component: () => import('./views/OrderDetailView.vue') },
  { path: '/admin/orders', name: 'admin-orders', component: () => import('./views/admin/AdminOrdersView.vue'), meta: { admin: true } },
  { path: '/admin/orders/:id', name: 'admin-order-detail', component: () => import('./views/admin/AdminOrderDetailView.vue'), meta: { admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
];

export const router = createRouter({ history: createWebHistory(), routes });

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!auth.loaded) await auth.fetchMe();

  if (to.meta.admin) {
    if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } };
    if (!auth.isAdmin) return { name: 'profile' };
    return true;
  }
  if (to.meta.public) return true;
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } };
  return true;
});
