export type OrderType = 'DINE_IN' | 'TAKE_AWAY';

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PREPARING'
  | 'READY'
  | 'COMPLETED'
  | 'CANCELLED';

export interface WebResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface MenuItem {
  id: number;
  name: string;
  category: string;
  description: string;
  price: number;
  imageUrl?: string | null;
  isAvailable: boolean;
}

export interface TableContext {
  outletId: number;
  outletName: string;
  outletIsOpen: boolean;
  taxRate: number;            // basis points (e.g. 1000 = 10%)
  serviceChargeRate: number;  // basis points (e.g. 500 = 5%)
  tableNumber: string | null;
  tableIsActive: boolean;
}

export interface OrderItemRequest {
  menuItemId: number;
  quantity: number;
  notes?: string | null;
}

export interface CreateOrderRequest {
  outletId: number;
  orderType: OrderType;
  tableNumber?: string | null;
  customerName?: string | null;
  notes?: string | null;
  items: OrderItemRequest[];
}

export interface OrderLineResponse {
  id: number;
  menuItemId: number;
  itemName: string;
  price: number;
  quantity: number;
  subtotal: number;
  notes?: string | null;
}

export interface OrderResponse {
  id: string; // UUID
  outletId: number;
  orderType: OrderType;
  tableNumber?: string | null;
  queueNumber?: number | null;
  customerName?: string | null;
  subtotal: number;
  taxAmount: number;
  serviceCharge: number;
  total: number;
  status: OrderStatus;
  notes?: string | null;
  version: number;
  createdAt: string;
  items: OrderLineResponse[];
}

export interface CreatePaymentRequest {
  provider: 'MIDTRANS' | 'XENDIT';
}

export interface PaymentResponse {
  id: number;
  orderId: string;
  provider: string;
  reference: string;
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED';
  amount: number;
  qrString: string;
  expiresAt: string;
  paidAt?: string | null;
}

export interface CartItem {
  menuItem: MenuItem;
  quantity: number;
  notes?: string;
}
