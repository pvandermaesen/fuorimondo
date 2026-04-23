export type Locale = 'FR' | 'IT' | 'EN';
export type UserStatus = 'WAITING_LIST' | 'ALLOCATAIRE_PENDING' | 'ALLOCATAIRE' | 'SUSPENDED';
export type UserRole = 'USER' | 'ADMIN';
export type Civility = 'MR' | 'MRS' | 'OTHER' | 'NONE';
export type TierCode = 'TIER_1' | 'TIER_2' | 'TIER_3';
export type AddressType = 'BILLING' | 'SHIPPING';

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  civility: Civility;
  birthDate: string | null;
  phone: string | null;
  country: string;
  city: string;
  status: UserStatus;
  role: UserRole;
  tierCode: TierCode | null;
  locale: Locale;
}

export interface AdminUserResponse extends UserResponse {
  referrerInfo: string | null;
  adminNotes: string | null;
  createdAt: string;
  invitationCode: string | null;
  invitationCodeExpiresAt: string | null;
  invitationCodeUsedAt: string | null;
}

export interface AddressResponse {
  id: string;
  type: AddressType;
  fullName: string;
  street: string;
  streetExtra: string | null;
  postalCode: string;
  city: string;
  country: string;
  isDefault: boolean;
}

export interface AddressRequest {
  type: AddressType;
  fullName: string;
  street: string;
  streetExtra?: string | null;
  postalCode: string;
  city: string;
  country: string;
  isDefault: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterWaitingListRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  country: string;
  city: string;
  referrerInfo?: string;
  locale: Locale;
  password: string;
  acceptTerms: boolean;
  acceptPrivacy: boolean;
}

export interface ActivateRequest {
  email: string;
  code: string;
}

export interface SetPasswordRequest {
  email: string;
  code: string;
  password: string;
}

export interface PasswordResetRequest { email: string; }
export interface PasswordResetConfirmRequest { token: string; password: string; }

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  civility: Civility;
  birthDate?: string | null;
  phone?: string;
  country: string;
  city: string;
  locale: Locale;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CreateAllocataireRequest {
  email: string;
  firstName: string;
  lastName: string;
  civility: Civility;
  birthDate?: string | null;
  phone?: string;
  country: string;
  city: string;
  tierCode: TierCode;
  locale: Locale;
  adminNotes?: string;
}

export interface CreateAllocataireResponse {
  user: AdminUserResponse;
  code: string;
}

export interface UpdateUserByAdminRequest {
  status?: UserStatus;
  tierCode?: TierCode;
  adminNotes?: string;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface LegalResponse {
  slug: string;
  locale: Locale;
  markdown: string;
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
}

export interface ProductRequest {
  name: string;
  description: string | null;
  priceEur: string;
  delivery: boolean;
  weightKg: string | null;
  tiers: TierCode[];
  saleStartAt: string;
  saleEndAt: string | null;
  stock: number | null;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  delivery: boolean;
  weightKg: string | null;
  tiers: TierCode[];
  saleStartAt: string;
  saleEndAt: string | null;
  stock: number | null;
  createdAt: string;
  updatedAt: string;
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

export interface PublicProductResponse {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  weightKg: string | null;
  delivery: boolean;
  tiers: TierCode[];
  saleStartAt: string;
  saleEndAt: string | null;
  stockRemaining: number | null;
}

export interface ProductSnapshot {
  id: string;
  name: string;
  description: string | null;
  priceEur: string;
  photoFilename: string | null;
  weightKg: string | null;
  delivery: boolean;
  tiers: TierCode[];
}

export interface ShippingSnapshot {
  fullName: string;
  street: string;
  streetExtra: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface CreateOrderRequest { productId: string; shippingAddressId?: string | null; }
export interface CreateOrderResponse { orderId: string; checkoutUrl: string; }

export interface OrderResponse {
  id: string;
  product: ProductSnapshot;
  unitPriceEur: string;
  totalEur: string;
  shippingAddress: ShippingSnapshot | null;
  status: OrderStatus;
  mollieCheckoutUrl: string | null;
  expiresAt: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface AdminOrderResponse {
  id: string;
  userId: string;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  product: ProductSnapshot;
  totalEur: string;
  shipping: ShippingSnapshot | null;
  status: OrderStatus;
  molliePaymentId: string | null;
  createdAt: string;
  paidAt: string | null;
}
