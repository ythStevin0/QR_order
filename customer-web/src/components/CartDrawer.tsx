import React, { useState } from 'react';
import { X, Trash2, Plus, Minus, User, MessageSquare, AlertCircle, ShoppingBag, UtensilsCrossed } from 'lucide-react';
import type { CartItem, TableContext, OrderType } from '../types/api';
import { formatRupiah } from '../utils/format';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartItem[];
  context: TableContext | null;
  orderType: OrderType;
  onUpdateQuantity: (menuItemId: number, delta: number) => void;
  onRemoveItem: (menuItemId: number) => void;
  onSubmitOrder: (customerName: string, notes: string) => Promise<void>;
  isSubmitting: boolean;
  submitError?: string | null;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  cart,
  context,
  orderType,
  onUpdateQuantity,
  onRemoveItem,
  onSubmitOrder,
  isSubmitting,
  submitError,
}) => {
  const [customerName, setCustomerName] = useState('');
  const [orderNotes, setOrderNotes] = useState('');
  const [nameError, setNameError] = useState(false);

  if (!isOpen) return null;

  const subtotal = cart.reduce((sum, item) => sum + item.menuItem.price * item.quantity, 0);
  const taxRate = context?.taxRate || 0; // basis points e.g. 1000 = 10%
  const serviceRate = context?.serviceChargeRate || 0; // e.g. 500 = 5%

  const taxAmount = Math.round((subtotal * taxRate) / 10000);
  const serviceAmount = Math.round((subtotal * serviceRate) / 10000);
  const grandTotal = subtotal + taxAmount + serviceAmount;

  const handleCheckout = () => {
    if (orderType === 'TAKE_AWAY' && !customerName.trim()) {
      setNameError(true);
      return;
    }
    setNameError(false);
    onSubmitOrder(customerName.trim(), orderNotes.trim());
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-stone-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col max-h-[92vh] overflow-hidden">
        {/* Header */}
        <div className="p-4 border-b border-stone-100 flex items-center justify-between bg-stone-50/50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
              {orderType === 'DINE_IN' ? <UtensilsCrossed className="w-4 h-4" /> : <ShoppingBag className="w-4 h-4" />}
            </div>
            <div>
              <h2 className="font-extrabold text-base text-stone-900 leading-tight">Keranjang Pesanan</h2>
              <span className="text-xs text-stone-500 font-medium">
                {orderType === 'DINE_IN' ? `Makan di Meja ${context?.tableNumber || '-'}` : 'Bungkus / Take Away'}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-200/60 text-stone-600 hover:bg-stone-300 flex items-center justify-center cursor-pointer transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Scrollable Items */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {cart.length === 0 ? (
            <div className="text-center py-12 text-stone-400">
              <ShoppingBag className="w-12 h-12 mx-auto stroke-1 opacity-50 mb-2" />
              <p className="text-sm font-semibold">Keranjang Anda masih kosong</p>
              <p className="text-xs mt-1">Silakan pilih menu favorit Anda di katalog</p>
            </div>
          ) : (
            cart.map((item) => (
              <div
                key={item.menuItem.id}
                className="p-3 bg-stone-50 rounded-2xl border border-stone-200/80 flex items-start justify-between gap-3"
              >
                <div className="flex-1 min-w-0">
                  <h4 className="font-bold text-sm text-stone-900 leading-tight truncate">
                    {item.menuItem.name}
                  </h4>
                  <span className="text-xs font-semibold text-orange-600 mt-0.5 block">
                    {formatRupiah(item.menuItem.price)}
                  </span>
                  {item.notes && (
                    <p className="text-[11px] text-stone-500 bg-white/80 border border-stone-200/60 px-2 py-0.5 rounded-md mt-1 inline-block italic">
                      "{item.notes}"
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1.5 bg-white border border-stone-200 rounded-xl px-1.5 py-0.5 shadow-2xs">
                    <button
                      onClick={() => onUpdateQuantity(item.menuItem.id, -1)}
                      className="w-6 h-6 rounded-lg text-stone-600 flex items-center justify-center font-bold text-xs hover:bg-stone-100 cursor-pointer active:scale-90 transition"
                    >
                      <Minus className="w-3.5 h-3.5" />
                    </button>
                    <span className="text-xs font-extrabold text-stone-900 min-w-4 text-center">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => onUpdateQuantity(item.menuItem.id, 1)}
                      className="w-6 h-6 rounded-lg bg-orange-600 text-white flex items-center justify-center font-bold text-xs shadow-2xs hover:bg-orange-700 cursor-pointer active:scale-90 transition"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <button
                    onClick={() => onRemoveItem(item.menuItem.id)}
                    className="w-7 h-7 rounded-lg text-stone-400 hover:text-red-500 hover:bg-red-50 flex items-center justify-center cursor-pointer transition"
                    title="Hapus menu"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}

          {cart.length > 0 && (
            <>
              {/* Customer Name Input (Mandatory for Takeaway) */}
              <div className="mt-4 pt-4 border-t border-stone-100">
                <label className="text-xs font-bold text-stone-800 flex items-center justify-between mb-1.5">
                  <span className="flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-orange-500" />
                    Nama Pemesan {orderType === 'TAKE_AWAY' && <span className="text-red-500">* (Wajib)</span>}
                  </span>
                  {orderType === 'DINE_IN' && <span className="text-[10px] text-stone-400">Opsional</span>}
                </label>
                <input
                  type="text"
                  value={customerName}
                  onChange={(e) => {
                    setCustomerName(e.target.value);
                    if (e.target.value.trim()) setNameError(false);
                  }}
                  placeholder={orderType === 'TAKE_AWAY' ? 'Masukkan nama Anda untuk dipanggil di kasir' : 'Nama Anda (opsional)'}
                  className={`w-full text-xs p-3 rounded-xl border ${
                    nameError ? 'border-red-500 ring-2 ring-red-100' : 'border-stone-200'
                  } focus:outline-hidden focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20`}
                />
                {nameError && (
                  <p className="text-[11px] text-red-500 mt-1 flex items-center gap-1 font-medium">
                    <AlertCircle className="w-3 h-3" />
                    Nama wajib diisi untuk pesanan Take Away agar kasir dapat memanggil antrean Anda.
                  </p>
                )}
              </div>

              {/* Order Notes */}
              <div className="mt-2">
                <label className="text-xs font-bold text-stone-800 flex items-center gap-1.5 mb-1.5">
                  <MessageSquare className="w-3.5 h-3.5 text-stone-400" />
                  <span>Catatan untuk Keseluruhan Pesanan</span>
                </label>
                <input
                  type="text"
                  value={orderNotes}
                  onChange={(e) => setOrderNotes(e.target.value)}
                  placeholder="Contoh: Jangan terlalu asin, sendok garpu 2 set..."
                  className="w-full text-xs p-3 rounded-xl border border-stone-200 focus:outline-hidden focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20"
                />
              </div>

              {/* Price Breakdown */}
              <div className="mt-4 p-3 bg-stone-50 rounded-2xl border border-stone-200/80 space-y-1.5 text-xs">
                <div className="flex justify-between text-stone-600">
                  <span>Subtotal</span>
                  <span className="font-semibold text-stone-900">{formatRupiah(subtotal)}</span>
                </div>
                {taxRate > 0 && (
                  <div className="flex justify-between text-stone-500">
                    <span>PB1 / Pajak Resto ({taxRate / 100}%)</span>
                    <span>{formatRupiah(taxAmount)}</span>
                  </div>
                )}
                {serviceRate > 0 && (
                  <div className="flex justify-between text-stone-500">
                    <span>Biaya Layanan ({serviceRate / 100}%)</span>
                    <span>{formatRupiah(serviceAmount)}</span>
                  </div>
                )}
                <div className="flex justify-between text-sm font-extrabold text-stone-900 pt-2 border-t border-stone-200">
                  <span>Total Pembayaran</span>
                  <span className="text-orange-600">{formatRupiah(grandTotal)}</span>
                </div>
              </div>
            </>
          )}

          {submitError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{submitError}</span>
            </div>
          )}
        </div>

        {/* Footer Submit */}
        {cart.length > 0 && (
          <div className="p-4 border-t border-stone-100 bg-white">
            <button
              onClick={handleCheckout}
              disabled={isSubmitting || (context && !context.outletIsOpen)}
              className="w-full py-3.5 bg-linear-to-r from-orange-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 disabled:opacity-50 active:scale-98 text-white font-extrabold text-sm rounded-2xl shadow-lg shadow-orange-600/30 flex items-center justify-center gap-2 cursor-pointer transition"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Memproses Pesanan...</span>
                </>
              ) : (
                <>
                  <span>Lanjut Bayar QRIS</span>
                  <span>•</span>
                  <span>{formatRupiah(grandTotal)}</span>
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
