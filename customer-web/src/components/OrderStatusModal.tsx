import React, { useEffect, useState } from 'react';
import confetti from 'canvas-confetti';
import {
  CheckCircle2,
  UtensilsCrossed,
  Sparkles,
  X,
  PlusCircle,
  Receipt,
} from 'lucide-react';
import type { OrderResponse, OrderStatus } from '../types/api';
import { formatRupiah } from '../utils/format';
import { api } from '../api/client';

interface OrderStatusModalProps {
  isOpen: boolean;
  onClose: () => void;
  order: OrderResponse | null;
  onNewOrder: () => void;
}

export const OrderStatusModal: React.FC<OrderStatusModalProps> = ({
  isOpen,
  onClose,
  order,
  onNewOrder,
}) => {
  const [currentOrder, setCurrentOrder] = useState<OrderResponse | null>(order);
  const [isConnected, setIsConnected] = useState(true);

  useEffect(() => {
    setCurrentOrder(order);
  }, [order]);

  // Subscribe to real-time status updates via SSE
  useEffect(() => {
    if (!isOpen || !order?.id) return;

    const unsubscribe = api.subscribeOrderStatus(
      order.id,
      (updatedOrder) => {
        setIsConnected(true);
        setCurrentOrder(updatedOrder);

        // Fire confetti celebration when status becomes READY!
        if (updatedOrder.status === 'READY') {
          confetti({
            particleCount: 80,
            spread: 70,
            origin: { y: 0.6 },
          });
        }
      },
      () => {
        setIsConnected(false);
      }
    );

    return () => {
      unsubscribe();
    };
  }, [isOpen, order?.id]);

  if (!isOpen || !currentOrder) return null;

  const isTakeaway = currentOrder.orderType === 'TAKE_AWAY';

  const steps: { key: OrderStatus; label: string; desc: string }[] = [
    { key: 'PAID', label: 'Lunas', desc: 'Pesanan telah dibayar & masuk sistem dapur' },
    { key: 'PREPARING', label: 'Dimasak', desc: 'Dapur sedang menyiapkan hidangan Anda' },
    { key: 'READY', label: 'Siap', desc: isTakeaway ? 'Silakan ambil di konter kasir' : 'Makanan diantar ke meja' },
    { key: 'COMPLETED', label: 'Selesai', desc: 'Pesanan telah selesai' },
  ];

  const getStepIndex = (status: OrderStatus): number => {
    switch (status) {
      case 'PENDING_PAYMENT': return -1;
      case 'PAID': return 0;
      case 'PREPARING': return 1;
      case 'READY': return 2;
      case 'COMPLETED': return 3;
      case 'CANCELLED': return -2;
    }
  };

  const currentStepIdx = getStepIndex(currentOrder.status);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-stone-900/70 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl relative max-h-[92vh] overflow-y-auto">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center cursor-pointer transition z-10"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Real-time SSE indicator */}
        <div className="flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full w-fit mb-3">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>{isConnected ? 'Terhubung Real-Time (SSE)' : 'Menghubungkan Ulang...'}</span>
        </div>

        {/* Takeaway: Queue Number Hero Card */}
        {isTakeaway ? (
          <div className="bg-linear-to-br from-orange-500 to-amber-500 text-white rounded-2xl p-4 text-center shadow-lg shadow-orange-500/20 mb-4 relative overflow-hidden">
            <Sparkles className="w-4 h-4 text-amber-200 absolute top-2 right-2 opacity-60" />
            <span className="text-xs font-bold uppercase tracking-wider text-orange-100">
              Nomor Antrean Anda
            </span>
            <div className="text-5xl font-black tracking-tight my-1 drop-shadow-xs">
              #{currentOrder.queueNumber ? String(currentOrder.queueNumber).padStart(2, '0') : '--'}
            </div>
            <p className="text-xs text-orange-100 font-medium">
              Atas nama: <strong className="text-white">{currentOrder.customerName || 'Pelanggan'}</strong>
            </p>
          </div>
        ) : (
          /* Dine-In: Table Confirmation Hero Card */
          <div className="bg-linear-to-br from-emerald-600 to-teal-600 text-white rounded-2xl p-4 text-center shadow-lg shadow-emerald-600/20 mb-4 relative overflow-hidden">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-100">
              Pesanan Makan di Tempat
            </span>
            <div className="text-3xl font-black tracking-tight my-1 flex items-center justify-center gap-2">
              <UtensilsCrossed className="w-6 h-6" />
              <span>Meja {currentOrder.tableNumber}</span>
            </div>
            <p className="text-xs text-emerald-100">
              Makanan akan diantar langsung oleh staf ke meja Anda
            </p>
          </div>
        )}

        {/* Status Tracker Stepper */}
        <div className="bg-stone-50 rounded-2xl p-4 border border-stone-200/80 mb-4">
          <div className="space-y-4">
            {steps.map((step, idx) => {
              const isPassed = currentStepIdx >= idx;
              const isCurrent = currentStepIdx === idx;

              return (
                <div key={step.key} className="flex items-start gap-3 relative">
                  {/* Connecting Line */}
                  {idx < steps.length - 1 && (
                    <div
                      className={`absolute left-3.5 top-7 bottom-0 w-0.5 -mb-4 ${
                        currentStepIdx > idx ? 'bg-emerald-500' : 'bg-stone-200'
                      }`}
                    />
                  )}

                  {/* Icon Node */}
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition-colors ${
                      isCurrent
                        ? 'bg-orange-600 text-white ring-4 ring-orange-100 shadow-xs animate-pulse-subtle'
                        : isPassed
                        ? 'bg-emerald-500 text-white'
                        : 'bg-stone-200 text-stone-400'
                    }`}
                  >
                    {isPassed ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                  </div>

                  {/* Text details */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4
                        className={`text-xs font-bold leading-tight ${
                          isCurrent
                            ? 'text-orange-700 text-sm'
                            : isPassed
                            ? 'text-stone-900'
                            : 'text-stone-400'
                        }`}
                      >
                        {step.label}
                      </h4>
                      {isCurrent && (
                        <span className="text-[10px] bg-orange-100 text-orange-700 font-extrabold px-2 py-0.5 rounded-full uppercase">
                          Sekarang
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-stone-500 mt-0.5">{step.desc}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Ordered Items Summary */}
        <div className="border-t border-stone-100 pt-3 text-left">
          <h5 className="text-xs font-bold text-stone-700 mb-2 flex items-center gap-1.5">
            <Receipt className="w-3.5 h-3.5 text-stone-400" />
            <span>Rincian Menu ({currentOrder.items?.length || 0})</span>
          </h5>
          <div className="space-y-1.5 max-h-36 overflow-y-auto text-xs text-stone-600 pr-1">
            {currentOrder.items?.map((item) => (
              <div key={item.id} className="flex justify-between items-center py-1 border-b border-stone-100/80">
                <div className="truncate pr-2">
                  <span className="font-semibold text-stone-800">{item.itemName}</span>
                  <span className="text-stone-400 ml-1.5">x{item.quantity}</span>
                  {item.notes && <span className="text-[10px] text-orange-600 block italic">"{item.notes}"</span>}
                </div>
                <span className="font-bold text-stone-900 shrink-0">
                  {formatRupiah(item.subtotal)}
                </span>
              </div>
            ))}
          </div>

          <div className="flex justify-between items-center mt-3 pt-2 border-t border-stone-200 text-sm font-extrabold text-stone-900">
            <span>Total Bayar</span>
            <span className="text-orange-600">{formatRupiah(currentOrder.total)}</span>
          </div>
        </div>

        {/* Action Button */}
        <div className="mt-5 pt-3 border-t border-stone-100 flex gap-2">
          <button
            onClick={onNewOrder}
            className="flex-1 py-3 bg-stone-100 hover:bg-stone-200 text-stone-800 font-bold text-xs rounded-2xl flex items-center justify-center gap-1.5 cursor-pointer transition active:scale-98"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Pesan Menu Lain</span>
          </button>
          <button
            onClick={onClose}
            className="flex-1 py-3 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-2xl flex items-center justify-center gap-1.5 shadow-md shadow-orange-600/25 cursor-pointer transition active:scale-98"
          >
            <span>Tutup</span>
          </button>
        </div>
      </div>
    </div>
  );
};
