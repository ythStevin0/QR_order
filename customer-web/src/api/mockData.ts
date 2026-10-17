import type {
  MenuItem,
  TableContext,
  CreateOrderRequest,
  OrderResponse,
  PaymentResponse,
} from '../types/api';

export const MOCK_OUTLET_CONTEXT: TableContext = {
  outletId: 1,
  outletName: 'Mie Gacoan — Sudirman',
  outletIsOpen: true,
  taxRate: 1000,           // 10%
  serviceChargeRate: 500,  // 5%
  tableNumber: '12',
  tableIsActive: true,
};

export const MOCK_MENU_ITEMS: MenuItem[] = [
  {
    id: 1,
    name: 'Mie Hompimpa (Pedas Gurih)',
    category: 'Mie Pedas',
    description: 'Mie kenyal pedas asin gurih dengan topping ayam cincang dan pangsit krispi.',
    price: 15000,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 2,
    name: 'Mie Suit (Original Gurih)',
    category: 'Mie Pedas',
    description: 'Mie original tanpa cabai dengan rasa asin gurih khas bumbu rahasia.',
    price: 15000,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 3,
    name: 'Mie Gacoan (Pedas Manis)',
    category: 'Mie Pedas',
    description: 'Mie pedas manis dengan taburan ayam cincang dan kerupuk pangsit renyah.',
    price: 15500,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 4,
    name: 'Pangsit Goreng (Isi 3)',
    category: 'Dimsum & Camilan',
    description: 'Pangsit renyah isi olahan daging ayam dan udang dengan saus asam manis.',
    price: 12000,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 5,
    name: 'Udang Keju (Isi 3)',
    category: 'Dimsum & Camilan',
    description: 'Olahan udang dengan lelehan keju mozarella di dalam, dibalut tepung roti renyah.',
    price: 14500,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 6,
    name: 'Siomay Ayam (Isi 3)',
    category: 'Dimsum & Camilan',
    description: 'Siomay kukus lembut daging ayam pilihan disajikan dengan saus bangkok.',
    price: 12500,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 7,
    name: 'Es Gobak Sodor',
    category: 'Minuman Segar',
    description: 'Es manis segar dengan campuran buah cincau, jeli, dan susu kental manis.',
    price: 9500,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 8,
    name: 'Es Petak Umpet',
    category: 'Minuman Segar',
    description: 'Minuman tropis buah belimbing dan jeruk nipis yang menyegarkan.',
    price: 9500,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 9,
    name: 'Es Teh Manis',
    category: 'Minuman Segar',
    description: 'Teh melati seduh harum dengan es batu segar pelepas dahaga.',
    price: 5000,
    imageUrl: null,
    isAvailable: true,
  },
  {
    id: 10,
    name: 'Air Mineral Prima 600ml',
    category: 'Minuman Segar',
    description: 'Air mineral murni dingin dan higienis.',
    price: 4500,
    imageUrl: null,
    isAvailable: true,
  },
];

// Mock Order & Payment Storage
const mockOrders = new Map<string, OrderResponse>();
const mockPayments = new Map<string, PaymentResponse>();
const orderListeners = new Map<string, Set<(order: OrderResponse) => void>>();
let dailyQueueCounter = 1;

export const mockApi = {
  getOutletContext(outletId: number): TableContext {
    return { ...MOCK_OUTLET_CONTEXT, outletId, tableNumber: null, tableIsActive: false };
  },

  getTableContext(outletId: number, tableNumber: string): TableContext {
    return { ...MOCK_OUTLET_CONTEXT, outletId, tableNumber, tableIsActive: true };
  },

  getActiveMenu(_outletId: number): MenuItem[] {
    return MOCK_MENU_ITEMS;
  },

  createOrder(request: CreateOrderRequest): OrderResponse {
    const id = crypto.randomUUID();
    const items = request.items.map((item, idx) => {
      const found = MOCK_MENU_ITEMS.find((m) => m.id === item.menuItemId);
      const price = found?.price || 15000;
      return {
        id: idx + 1,
        menuItemId: item.menuItemId,
        itemName: found?.name || `Menu #${item.menuItemId}`,
        price,
        quantity: item.quantity,
        subtotal: price * item.quantity,
        notes: item.notes || null,
      };
    });

    const subtotal = items.reduce((acc, it) => acc + it.subtotal, 0);
    const taxAmount = Math.round((subtotal * MOCK_OUTLET_CONTEXT.taxRate) / 10000);
    const serviceCharge = Math.round((subtotal * MOCK_OUTLET_CONTEXT.serviceChargeRate) / 10000);
    const total = subtotal + taxAmount + serviceCharge;

    const order: OrderResponse = {
      id,
      outletId: request.outletId,
      orderType: request.orderType,
      tableNumber: request.orderType === 'DINE_IN' ? request.tableNumber || '12' : null,
      queueNumber: null, // assigned on payment!
      customerName: request.customerName || null,
      subtotal,
      taxAmount,
      serviceCharge,
      total,
      status: 'PENDING_PAYMENT',
      notes: request.notes || null,
      version: 1,
      createdAt: new Date().toISOString(),
      items,
    };

    mockOrders.set(id, order);
    return order;
  },

  getOrderById(orderId: string): OrderResponse {
    const order = mockOrders.get(orderId);
    if (!order) {
      throw new Error(`Pesanan dengan ID ${orderId} tidak ditemukan`);
    }
    return order;
  },

  createPayment(orderId: string): PaymentResponse {
    const order = mockOrders.get(orderId);
    if (!order) throw new Error('Order not found');

    const reference = 'QRIS-DEMO-' + orderId.substring(0, 8);
    const payment: PaymentResponse = {
      id: Date.now(),
      orderId,
      provider: 'MIDTRANS',
      reference,
      status: 'PENDING',
      amount: order.total,
      qrString: `00020101021226680016ID.CO.QRIS.WWW01189360052300000000015204581253033605802ID5910QR_ORDER6007JAKARTA62${reference.length}${reference}6304ABCD`,
      expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
    };

    mockPayments.set(reference, payment);
    return payment;
  },

  simulatePaymentWebhook(reference: string): void {
    const payment = mockPayments.get(reference);
    if (!payment) return;

    payment.status = 'PAID';
    payment.paidAt = new Date().toISOString();

    const order = mockOrders.get(payment.orderId);
    if (order) {
      order.status = 'PAID';
      if (order.orderType === 'TAKE_AWAY' && !order.queueNumber) {
        order.queueNumber = dailyQueueCounter++;
      }
      this.notifyOrderListeners(order);

      // Auto simulate Kitchen progression in Demo mode:
      // PAID -> PREPARING (after 3s) -> READY (after 7s) -> COMPLETED (after 15s)
      setTimeout(() => {
        if (order.status === 'PAID') {
          order.status = 'PREPARING';
          this.notifyOrderListeners(order);
        }
      }, 3000);

      setTimeout(() => {
        if (order.status === 'PREPARING') {
          order.status = 'READY';
          this.notifyOrderListeners(order);
        }
      }, 8000);
    }
  },

  notifyOrderListeners(order: OrderResponse) {
    const listeners = orderListeners.get(order.id);
    if (listeners) {
      listeners.forEach((fn) => fn({ ...order }));
    }
  },

  subscribeOrderStatus(
    orderId: string,
    onStatusUpdate: (order: OrderResponse) => void
  ): () => void {
    if (!orderListeners.has(orderId)) {
      orderListeners.set(orderId, new Set());
    }
    const set = orderListeners.get(orderId)!;
    set.add(onStatusUpdate);

    return () => {
      set.delete(onStatusUpdate);
    };
  },
};
