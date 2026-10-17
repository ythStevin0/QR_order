import type {
  MenuItem,
  TableContext,
  CreateOrderRequest,
  OrderResponse,
  PaymentResponse,
  WebResponse,
} from '../types/api';
import { mockApi } from './mockData';

const BASE_URL = '/api/v1';

let isMockMode = false;

export function isUsingMock(): boolean {
  return isMockMode;
}

export function setUsingMock(value: boolean): void {
  isMockMode = value;
}

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  if (isMockMode) {
    throw new Error('FORCE_MOCK');
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...options?.headers,
      },
    });

    if (!response.ok) {
      let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
      try {
        const errBody = await response.json();
        if (errBody.detail) {
          errorMsg = errBody.detail;
        } else if (errBody.message) {
          errorMsg = errBody.message;
        }
      } catch {
        // fallback
      }
      throw new Error(errorMsg);
    }

    const result: WebResponse<T> = await response.json();
    return result.data;
  } catch (err: any) {
    // If backend is down (Vite returns 500 on proxy ECONNREFUSED or network failure)
    console.warn(`[API] Backend error (${err.message}). Activating Demo/Mock Mode.`);
    isMockMode = true;
    throw err;
  }
}

export const api = {
  // 1. Context APIs
  async getOutletContext(outletId: number): Promise<TableContext> {
    try {
      return await fetchJson<TableContext>(`${BASE_URL}/outlets/${outletId}`);
    } catch {
      return mockApi.getOutletContext(outletId);
    }
  },

  async getTableContext(outletId: number, tableNumber: string): Promise<TableContext> {
    try {
      return await fetchJson<TableContext>(`${BASE_URL}/outlets/${outletId}/tables/${encodeURIComponent(tableNumber)}`);
    } catch {
      return mockApi.getTableContext(outletId, tableNumber);
    }
  },

  // 2. Menu Catalog
  async getActiveMenu(outletId: number): Promise<MenuItem[]> {
    try {
      return await fetchJson<MenuItem[]>(`${BASE_URL}/outlets/${outletId}/menu`);
    } catch {
      return mockApi.getActiveMenu(outletId);
    }
  },

  // 3. Ordering
  async createOrder(request: CreateOrderRequest, idempotencyKey: string): Promise<OrderResponse> {
    try {
      return await fetchJson<OrderResponse>(`${BASE_URL}/orders`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
        body: JSON.stringify(request),
      });
    } catch {
      return mockApi.createOrder(request);
    }
  },

  async getOrderById(orderId: string): Promise<OrderResponse> {
    try {
      return await fetchJson<OrderResponse>(`${BASE_URL}/orders/${orderId}`);
    } catch {
      return mockApi.getOrderById(orderId);
    }
  },

  // 4. Payment
  async createPayment(orderId: string, provider: 'MIDTRANS' | 'XENDIT' = 'MIDTRANS'): Promise<PaymentResponse> {
    try {
      return await fetchJson<PaymentResponse>(`${BASE_URL}/orders/${orderId}/payment`, {
        method: 'POST',
        body: JSON.stringify({ provider }),
      });
    } catch {
      return mockApi.createPayment(orderId);
    }
  },

  // 5. Dev Sandbox Webhook simulation
  async simulatePaymentWebhook(reference: string): Promise<void> {
    try {
      if (isMockMode) {
        mockApi.simulatePaymentWebhook(reference);
        return;
      }

      const response = await fetch(`${BASE_URL}/payments/webhook`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reference,
          transactionStatus: 'settlement',
          signature: 'simulated-dev-signature',
        }),
      });

      if (!response.ok) {
        mockApi.simulatePaymentWebhook(reference);
      }
    } catch {
      mockApi.simulatePaymentWebhook(reference);
    }
  },

  // 6. SSE Real-time Order Stream
  subscribeOrderStatus(
    orderId: string,
    onStatusUpdate: (order: OrderResponse) => void,
    onError?: (error: Event) => void
  ): () => void {
    if (isMockMode) {
      return mockApi.subscribeOrderStatus(orderId, onStatusUpdate);
    }

    try {
      const sseUrl = `${BASE_URL}/orders/${orderId}/stream`;
      const eventSource = new EventSource(sseUrl);

      eventSource.addEventListener('status', (event: MessageEvent) => {
        try {
          const orderData = JSON.parse(event.data) as OrderResponse;
          onStatusUpdate(orderData);
        } catch (err) {
          console.error('Failed to parse SSE order event payload', err);
        }
      });

      eventSource.onerror = (err) => {
        console.warn('[SSE] Backend SSE failed, falling back to mock stream');
        eventSource.close();
        if (onError) onError(err);
        return mockApi.subscribeOrderStatus(orderId, onStatusUpdate);
      };

      return () => {
        eventSource.close();
      };
    } catch {
      return mockApi.subscribeOrderStatus(orderId, onStatusUpdate);
    }
  },
};
