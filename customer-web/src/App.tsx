import { useState, useEffect, useMemo } from 'react';
import {
  Utensils,
  Search,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import type {
  MenuItem,
  TableContext,
  CartItem,
  OrderType,
  OrderResponse,
  PaymentResponse,
} from './types/api';
import { api, isUsingMock } from './api/client';
import { generateIdempotencyKey, saveActiveOrder, getActiveOrder, clearActiveOrder } from './utils/format';
import { Header } from './components/Header';
import { CategoryNav } from './components/CategoryNav';
import { MenuItemCard } from './components/MenuItemCard';
import { ItemCustomizeModal } from './components/ItemCustomizeModal';
import { FloatingCartBar } from './components/FloatingCartBar';
import { CartDrawer } from './components/CartDrawer';
import { PaymentModal } from './components/PaymentModal';
import { OrderStatusModal } from './components/OrderStatusModal';

export function App() {
  // 1. Route / Context Parsing
  const [outletId, setOutletId] = useState<number>(1);
  const [orderType, setOrderType] = useState<OrderType>('DINE_IN');
  const [tableNumber, setTableNumber] = useState<string>('12');

  // 2. Data State
  const [context, setContext] = useState<TableContext | null>(null);
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // 3. Filter & Search
  const [selectedCategory, setSelectedCategory] = useState<string>('Semua');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // 4. Cart State
  const [cart, setCart] = useState<CartItem[]>([]);

  // 5. Active Order & Modals
  const [activeOrder, setActiveOrder] = useState<OrderResponse | null>(null);
  const [activePayment, setActivePayment] = useState<PaymentResponse | null>(null);

  const [customizingItem, setCustomizingItem] = useState<MenuItem | null>(null);
  const [isCartOpen, setIsCartOpen] = useState<boolean>(false);
  const [isPaymentOpen, setIsPaymentOpen] = useState<boolean>(false);
  const [isStatusOpen, setIsStatusOpen] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Parse URL on mount
  useEffect(() => {
    const parseUrl = () => {
      const pathname = window.location.pathname;
      const search = new URLSearchParams(window.location.search);
      const hash = window.location.hash;

      const pathOrHash = hash ? hash.replace('#', '') : pathname;

      // Match /o/:outletId/meja/:tableNumber
      const dineInMatch = pathOrHash.match(/\/o\/(\d+)\/meja\/([^/?#]+)/i);
      // Match /o/:outletId/takeaway
      const takeawayMatch = pathOrHash.match(/\/o\/(\d+)\/takeaway/i);

      if (dineInMatch) {
        setOutletId(Number(dineInMatch[1]));
        setOrderType('DINE_IN');
        setTableNumber(decodeURIComponent(dineInMatch[2]));
      } else if (takeawayMatch) {
        setOutletId(Number(takeawayMatch[1]));
        setOrderType('TAKE_AWAY');
        setTableNumber('');
      } else if (search.get('outlet')) {
        setOutletId(Number(search.get('outlet')));
        if (search.get('type') === 'takeaway') {
          setOrderType('TAKE_AWAY');
          setTableNumber('');
        } else {
          setOrderType('DINE_IN');
          setTableNumber(search.get('table') || '12');
        }
      }
    };

    parseUrl();
  }, []);

  // Restore Active Order session from localStorage if present
  useEffect(() => {
    const stored = getActiveOrder();
    if (stored) {
      api.getOrderById(stored.orderId)
        .then((orderData) => {
          if (orderData.status !== 'COMPLETED' && orderData.status !== 'CANCELLED') {
            setActiveOrder(orderData);
          } else {
            clearActiveOrder();
          }
        })
        .catch(() => {
          clearActiveOrder();
        });
    }
  }, []);

  // Fetch Outlet Context & Menu
  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // 1. Fetch context
      let ctx: TableContext;
      if (orderType === 'DINE_IN' && tableNumber) {
        ctx = await api.getTableContext(outletId, tableNumber);
      } else {
        ctx = await api.getOutletContext(outletId);
      }
      setContext(ctx);

      // 2. Fetch active menu
      const items = await api.getActiveMenu(outletId);
      setMenu(items);
    } catch (err: any) {
      console.error('Failed to load menu/context', err);
      setError(err.message || 'Gagal memuat menu outlet');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [outletId, orderType, tableNumber]);

  // Categories list & counts
  const categories = useMemo(() => {
    const set = new Set<string>();
    menu.forEach((item) => {
      if (item.category) set.add(item.category);
    });
    return ['Semua', ...Array.from(set)];
  }, [menu]);

  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = { Semua: menu.length };
    menu.forEach((item) => {
      counts[item.category] = (counts[item.category] || 0) + 1;
    });
    return counts;
  }, [menu]);

  // Filtered menu
  const filteredMenu = useMemo(() => {
    return menu.filter((item) => {
      const matchCategory =
        selectedCategory === 'Semua' || item.category === selectedCategory;
      const matchSearch =
        !searchQuery.trim() ||
        item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.description?.toLowerCase().includes(searchQuery.toLowerCase());
      return matchCategory && matchSearch;
    });
  }, [menu, selectedCategory, searchQuery]);

  // Cart operations
  const handleAddToCart = (item: MenuItem) => {
    setCustomizingItem(item);
  };

  const handleConfirmCustomize = (item: MenuItem, quantity: number, notes: string) => {
    setCart((prev) => {
      const existingIdx = prev.findIndex(
        (c) => c.menuItem.id === item.id && (c.notes || '') === (notes || '')
      );
      if (existingIdx >= 0) {
        const updated = [...prev];
        updated[existingIdx].quantity += quantity;
        return updated;
      }
      return [...prev, { menuItem: item, quantity, notes: notes || undefined }];
    });
    setCustomizingItem(null);
  };

  const handleUpdateQuantity = (menuItemId: number, delta: number) => {
    setCart((prev) => {
      return prev
        .map((item) => {
          if (item.menuItem.id === menuItemId) {
            const nextQty = item.quantity + delta;
            return nextQty > 0 ? { ...item, quantity: nextQty } : null;
          }
          return item;
        })
        .filter(Boolean) as CartItem[];
    });
  };

  const handleRemoveItem = (menuItemId: number) => {
    setCart((prev) => prev.filter((i) => i.menuItem.id !== menuItemId));
  };

  const cartTotalItems = useMemo(
    () => cart.reduce((sum, item) => sum + item.quantity, 0),
    [cart]
  );

  const cartSubtotal = useMemo(
    () => cart.reduce((sum, item) => sum + item.menuItem.price * item.quantity, 0),
    [cart]
  );

  // Submit Order & Generate QRIS
  const handleSubmitOrder = async (customerName: string, notes: string) => {
    try {
      setIsSubmitting(true);
      setSubmitError(null);

      const idempotencyKey = generateIdempotencyKey();
      const orderPayload = {
        outletId,
        orderType,
        tableNumber: orderType === 'DINE_IN' ? tableNumber : null,
        customerName: customerName || null,
        notes: notes || null,
        items: cart.map((c) => ({
          menuItemId: c.menuItem.id,
          quantity: c.quantity,
          notes: c.notes || null,
        })),
      };

      // 1. Create order
      const createdOrder = await api.createOrder(orderPayload, idempotencyKey);
      setActiveOrder(createdOrder);

      // Save to localStorage
      saveActiveOrder({
        orderId: createdOrder.id,
        outletId: createdOrder.outletId,
        orderType: createdOrder.orderType,
        tableNumber: createdOrder.tableNumber,
        customerName: createdOrder.customerName,
        createdAt: Date.now(),
      });

      // 2. Request QRIS Payment
      const paymentResponse = await api.createPayment(createdOrder.id, 'MIDTRANS');
      setActivePayment(paymentResponse);

      // 3. Clear cart & transition modals
      setCart([]);
      setIsCartOpen(false);
      setIsPaymentOpen(true);
    } catch (err: any) {
      console.error('Order checkout failed', err);
      setSubmitError(err.message || 'Gagal membuat pesanan');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePaymentSuccess = (paidOrder: OrderResponse) => {
    setActiveOrder(paidOrder);
    setIsPaymentOpen(false);
    setIsStatusOpen(true);
  };

  return (
    <div className="min-h-screen bg-stone-100 flex flex-col justify-between">
      <div className="w-full max-w-md mx-auto bg-stone-50 min-h-screen shadow-2xl flex flex-col pb-28 relative">
        {/* Header */}
        <Header
          context={context}
          orderType={orderType}
          activeOrderId={activeOrder?.id}
          onViewActiveOrder={() => setIsStatusOpen(true)}
          isDemo={isUsingMock()}
        />

        {/* Loading Screen */}
        {isLoading && (
          <div className="flex-1 flex flex-col items-center justify-center py-20 px-4 text-center">
            <div className="w-12 h-12 border-3 border-orange-200 border-t-orange-600 rounded-full animate-spin mb-4" />
            <h3 className="font-bold text-stone-800 text-sm">Menyiapkan Menu Outlet...</h3>
            <p className="text-xs text-stone-400 mt-1">Mohon tunggu sebentar</p>
          </div>
        )}

        {/* Error Screen */}
        {!isLoading && error && (
          <div className="flex-1 flex flex-col items-center justify-center py-16 px-6 text-center">
            <div className="w-14 h-14 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mb-4">
              <AlertCircle className="w-8 h-8" />
            </div>
            <h3 className="font-extrabold text-stone-900 text-base">Tidak Dapat Membuka Menu</h3>
            <p className="text-xs text-stone-500 mt-2 max-w-xs">{error}</p>
            <button
              onClick={loadData}
              className="mt-6 px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl flex items-center gap-2 shadow-md shadow-orange-600/30 cursor-pointer active:scale-95 transition"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Coba Lagi</span>
            </button>
          </div>
        )}

        {/* Main Content */}
        {!isLoading && !error && (
          <>
            {/* Search Input */}
            <div className="px-4 pt-3 pb-1">
              <div className="relative">
                <Search className="w-4 h-4 text-stone-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Cari makanan atau minuman..."
                  className="w-full pl-9 pr-4 py-2.5 bg-white border border-stone-200/90 rounded-2xl text-xs placeholder:text-stone-400 focus:outline-hidden focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 shadow-2xs"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="text-xs text-stone-400 hover:text-stone-600 absolute right-3 top-1/2 -translate-y-1/2 font-bold px-1"
                  >
                    ×
                  </button>
                )}
              </div>
            </div>

            {/* Category Navigation */}
            <CategoryNav
              categories={categories}
              selectedCategory={selectedCategory}
              onSelectCategory={setSelectedCategory}
              categoryCounts={categoryCounts}
            />

            {/* Menu List */}
            <main className="px-4 py-3 flex-1">
              {filteredMenu.length === 0 ? (
                <div className="text-center py-16 text-stone-400">
                  <Utensils className="w-10 h-10 mx-auto opacity-30 stroke-1 mb-2" />
                  <p className="text-sm font-semibold text-stone-600">Menu tidak ditemukan</p>
                  <p className="text-xs mt-1">Coba kata kunci atau kategori lainnya</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {filteredMenu.map((item) => {
                    const cartQty = cart
                      .filter((c) => c.menuItem.id === item.id)
                      .reduce((sum, c) => sum + c.quantity, 0);

                    return (
                      <MenuItemCard
                        key={item.id}
                        item={item}
                        cartQuantity={cartQty}
                        onAddToCart={handleAddToCart}
                        onUpdateQuantity={(it, delta) => handleUpdateQuantity(it.id, delta)}
                      />
                    );
                  })}
                </div>
              )}
            </main>

            {/* Dev Mode Context Switcher (Bottom Helper Pill) */}
            <div className="px-4 py-2 mt-4 text-center">
              <div className="inline-flex items-center gap-1.5 p-1 bg-stone-200/70 rounded-full text-[11px] font-semibold text-stone-600">
                <span className="px-2 text-stone-400">Mode:</span>
                <button
                  onClick={() => {
                    setOrderType('DINE_IN');
                    setTableNumber('12');
                  }}
                  className={`px-2.5 py-1 rounded-full cursor-pointer transition ${
                    orderType === 'DINE_IN'
                      ? 'bg-white text-emerald-800 shadow-xs font-bold'
                      : 'hover:text-stone-900'
                  }`}
                >
                  Meja 12
                </button>
                <button
                  onClick={() => {
                    setOrderType('TAKE_AWAY');
                    setTableNumber('');
                  }}
                  className={`px-2.5 py-1 rounded-full cursor-pointer transition ${
                    orderType === 'TAKE_AWAY'
                      ? 'bg-white text-blue-800 shadow-xs font-bold'
                      : 'hover:text-stone-900'
                  }`}
                >
                  Take Away
                </button>
              </div>
            </div>
          </>
        )}

        {/* Floating Bottom Cart Bar */}
        <FloatingCartBar
          totalItems={cartTotalItems}
          subtotal={cartSubtotal}
          onOpenCart={() => setIsCartOpen(true)}
        />

        {/* Modals */}
        <ItemCustomizeModal
          item={customizingItem}
          onClose={() => setCustomizingItem(null)}
          onConfirm={handleConfirmCustomize}
        />

        <CartDrawer
          isOpen={isCartOpen}
          onClose={() => setIsCartOpen(false)}
          cart={cart}
          context={context}
          orderType={orderType}
          onUpdateQuantity={handleUpdateQuantity}
          onRemoveItem={handleRemoveItem}
          onSubmitOrder={handleSubmitOrder}
          isSubmitting={isSubmitting}
          submitError={submitError}
        />

        <PaymentModal
          isOpen={isPaymentOpen}
          onClose={() => setIsPaymentOpen(false)}
          order={activeOrder}
          payment={activePayment}
          onPaymentSuccess={handlePaymentSuccess}
        />

        <OrderStatusModal
          isOpen={isStatusOpen}
          onClose={() => setIsStatusOpen(false)}
          order={activeOrder}
          onNewOrder={() => {
            setIsStatusOpen(false);
            clearActiveOrder();
            setActiveOrder(null);
            setActivePayment(null);
          }}
        />
      </div>
    </div>
  );
}
