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

class ApiHttpError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiHttpError';
    this.status = status;
  }
}

async function fetchJson<T>(
  url: string,
  options?: RequestInit,
  allowMockOn5xx = false
): Promise<T> {
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
      throw new ApiHttpError(response.status, errorMsg);
    }

    const result: WebResponse<T> = await response.json();
    return result.data;
  } catch (err: any) {
    if (err instanceof ApiHttpError) {
      if (err.status >= 400 && err.status < 500) {
        throw err;
      }
      if (allowMockOn5xx && err.status >= 500) {
        console.warn(`[API] Backend unavailable (${err.message}). Activating Demo/Mock Mode.`);
        isMockMode = true;
      }
      throw err;
    }

    // Network failure / ECONNREFUSED
    console.warn(`[API] Network error (${err.message}). Activating Demo/Mock Mode.`);
    isMockMode = true;
    throw err;
  }
}

export const api = {
  // 1. Context APIs
  async getOutletContext(outletId: number): Promise<TableContext> {
    try {
      return await fetchJson<TableContext>(`${BASE_URL}/outlets/${outletId}`, undefined, true);
    } catch (err) {
      if (isMockMode) return mockApi.getOutletContext(outletId);
      throw err;
    }
  },

  async getTableContext(outletId: number, tableNumber: string): Promise<TableContext> {
    try {
      return await fetchJson<TableContext>(
        `${BASE_URL}/outlets/${outletId}/tables/${encodeURIComponent(tableNumber)}`,
        undefined,
        true
      );
    } catch (err) {
      if (isMockMode) return mockApi.getTableContext(outletId, tableNumber);
      throw err;
    }
  },

  // 2. Menu Catalog
  async getActiveMenu(outletId: number): Promise<MenuItem[]> {
    try {
      return await fetchJson<MenuItem[]>(`${BASE_URL}/outlets/${outletId}/menu`, undefined, true);
    } catch (err) {
      if (isMockMode) return mockApi.getActiveMenu(outletId);
      throw err;
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
    } catch (err) {
      if (isMockMode) return mockApi.createOrder(request);
      throw err;
    }
  },

  async getOrderById(orderId: string): Promise<OrderResponse> {
    try {
      return await fetchJson<OrderResponse>(`${BASE_URL}/orders/${orderId}`);
    } catch (err) {
      if (isMockMode) return mockApi.getOrderById(orderId);
      throw err;
    }
  },

  // 4. Payment
  async createPayment(orderId: string, provider: 'MIDTRANS' | 'XENDIT' = 'MIDTRANS'): Promise<PaymentResponse> {
    try {
      return await fetchJson<PaymentResponse>(`${BASE_URL}/orders/${orderId}/payment`, {
        method: 'POST',
        body: JSON.stringify({ provider }),
      });
    } catch (err) {
      if (isMockMode) return mockApi.createPayment(orderId);
      throw err;
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
          signatureKey: 'simulated-dev-signature',
        }),
      });

      if (!response.ok) {
        throw new ApiHttpError(response.status, `Webhook failed (${response.status})`);
      }
    } catch (err) {
      if (isMockMode) {
        mockApi.simulatePaymentWebhook(reference);
        return;
      }
      throw err;
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

    let fallbackUnsubscribe: (() => void) | null = null;
    try {
      const sseUrl = `${BASE_URL}/orders/${orderId}/stream`;
      const eventSource = new EventSource(sseUrl);

      const handleSsePayload = (event: MessageEvent) => {
        try {
          const payload = JSON.parse(event.data);
          if (payload && Array.isArray(payload.items)) {
            onStatusUpdate(payload as OrderResponse);
          } else {
            // Backend KitchenServiceImpl sends { orderId, status } on STATUS_CHANGE
            api.getOrderById(orderId)
              .then(onStatusUpdate)
              .catch((err) => console.error('Failed to refresh order after SSE event', err));
          }
        } catch (err) {
          console.error('Failed to parse SSE order event payload', err);
        }
      };

      eventSource.addEventListener('STATUS_CHANGE', handleSsePayload);
      eventSource.addEventListener('status', handleSsePayload);

      eventSource.onerror = (err) => {
        console.warn('[SSE] Backend SSE error', err);
        if (onError) onError(err);
        if (eventSource.readyState === EventSource.CLOSED && isMockMode) {
          fallbackUnsubscribe = mockApi.subscribeOrderStatus(orderId, onStatusUpdate);
        }
      };

      return () => {
        eventSource.close();
        if (fallbackUnsubscribe) fallbackUnsubscribe();
      };
    } catch {
      return mockApi.subscribeOrderStatus(orderId, onStatusUpdate);
    }
  },
};
