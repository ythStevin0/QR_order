export function formatRupiah(amount: number): string {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  }).format(amount);
}

export function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'idem-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
}

export interface StoredOrderSession {
  orderId: string;
  outletId: number;
  orderType: 'DINE_IN' | 'TAKE_AWAY';
  tableNumber?: string | null;
  customerName?: string | null;
  createdAt: number;
}

const STORAGE_KEY = 'qr_order_active_session';

export function saveActiveOrder(session: StoredOrderSession): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch (err) {
    console.warn('Failed to save order session to localStorage', err);
  }
}

export function getActiveOrder(): StoredOrderSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as StoredOrderSession;
    // Expire session after 24 hours
    if (Date.now() - session.createdAt > 24 * 60 * 60 * 1000) {
      clearActiveOrder();
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function clearActiveOrder(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (err) {
    console.warn('Failed to clear order session', err);
  }
}
