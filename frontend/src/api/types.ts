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
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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
