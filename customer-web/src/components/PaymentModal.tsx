import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, Clock, QrCode, Sparkles, AlertTriangle } from 'lucide-react';
import type { PaymentResponse, OrderResponse } from '../types/api';
import { formatRupiah } from '../utils/format';
import { api } from '../api/client';

interface PaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  order: OrderResponse | null;
  payment: PaymentResponse | null;
  onPaymentSuccess: (updatedOrder: OrderResponse) => void;
}

export const PaymentModal: React.FC<PaymentModalProps> = ({
  isOpen,
  onClose,
  order,
  payment,
  onPaymentSuccess,
}) => {
  const [timeLeft, setTimeLeft] = useState<number>(15 * 60); // 15 mins
  const [isSimulating, setIsSimulating] = useState(false);
  const [simError, setSimError] = useState<string | null>(null);

  // Expiration countdown
  useEffect(() => {
    if (!isOpen || !payment?.expiresAt) return;

    const expiresAtMs = new Date(payment.expiresAt).getTime();
    const updateCountdown = () => {
      const now = Date.now();
      const diff = Math.max(0, Math.floor((expiresAtMs - now) / 1000));
      setTimeLeft(diff);
    };

    updateCountdown();
    const interval = setInterval(updateCountdown, 1000);
    return () => clearInterval(interval);
  }, [isOpen, payment]);

  // Subscribe to SSE updates while payment modal is open
  useEffect(() => {
    if (!isOpen || !order?.id) return;

    const unsubscribe = api.subscribeOrderStatus(
      order.id,
      (updatedOrder) => {
        if (updatedOrder.status === 'PAID') {
          onPaymentSuccess(updatedOrder);
        }
      },
      (err) => {
        console.warn('SSE payment stream error', err);
      }
    );

    return () => {
      unsubscribe();
    };
  }, [isOpen, order?.id, onPaymentSuccess]);

  if (!isOpen || !order || !payment) return null;

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const isExpired = timeLeft <= 0;

  const handleSimulatePayment = async () => {
    try {
      setIsSimulating(true);
      setSimError(null);
      await api.simulatePaymentWebhook(payment.reference);
      // Wait for SSE broadcast, or fetch order status directly as backup
      setTimeout(async () => {
        try {
          const refreshed = await api.getOrderById(order.id);
          if (refreshed.status === 'PAID') {
            onPaymentSuccess(refreshed);
          }
        } catch {
          // ignore
        }
      }, 500);
    } catch (err: any) {
      setSimError(err.message || 'Gagal simulasi pembayaran');
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/70 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-sm bg-white rounded-3xl p-5 shadow-2xl relative text-center">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-600 flex items-center justify-center cursor-pointer transition"
        >
          <X className="w-4 h-4" />
        </button>

        {/* QRIS Header */}
        <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-50 border border-red-200 text-red-700 rounded-full text-xs font-bold mb-3">
          <QrCode className="w-3.5 h-3.5" />
          <span>QRIS Standar Pembayaran Nasional</span>
        </div>

        <h3 className="text-sm font-semibold text-stone-500">Total Pembayaran</h3>
        <p className="text-2xl font-black text-stone-900 mt-0.5 tracking-tight">
          {formatRupiah(payment.amount)}
        </p>

        {/* QR Code Container */}
        <div className="my-4 p-4 bg-white rounded-2xl border-2 border-dashed border-stone-300 flex flex-col items-center justify-center shadow-xs">
          {isExpired ? (
            <div className="py-12 text-red-600 space-y-2">
              <AlertTriangle className="w-10 h-10 mx-auto" />
              <p className="text-sm font-bold">QRIS Kedaluwarsa</p>
              <p className="text-xs text-stone-500">Silakan buat pesanan baru</p>
            </div>
          ) : (
            <>
              <div className="p-2 bg-white rounded-xl shadow-xs">
                <QRCodeSVG
                  value={payment.qrString}
                  size={190}
                  level="M"
                  includeMargin={false}
                />
              </div>
              <p className="text-[11px] text-stone-400 mt-2 font-mono">
                Ref: {payment.reference}
              </p>
            </>
          )}
        </div>

        {/* Countdown Timer */}
        {!isExpired && (
          <div className="flex items-center justify-center gap-1.5 text-xs font-semibold text-amber-700 bg-amber-50 py-1.5 px-3 rounded-xl border border-amber-200/80 mb-4">
            <Clock className="w-3.5 h-3.5 text-amber-600 animate-spin" style={{ animationDuration: '4s' }} />
            <span>Selesaikan pembayaran dalam</span>
            <span className="font-extrabold font-mono text-sm text-amber-900">
              {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
            </span>
          </div>
        )}

        <p className="text-[11px] text-stone-500 leading-relaxed mb-4">
          Buka aplikasi e-wallet (GoPay, OVO, ShopeePay, DANA) atau mobile banking pilihanmu lalu scan kode QR di atas.
        </p>

        {/* Dev Mode Simulation Button */}
        <div className="pt-3 border-t border-stone-100">
          <button
            onClick={handleSimulatePayment}
            disabled={isSimulating || isExpired}
            className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 active:scale-98 disabled:opacity-50 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow-sm cursor-pointer transition"
          >
            {isSimulating ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Memproses Webhook...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-3.5 h-3.5 text-emerald-200" />
                <span>Simulasi Bayar Berhasil (Demo Mode)</span>
              </>
            )}
          </button>
          {simError && (
            <p className="text-[11px] text-red-500 mt-1.5">{simError}</p>
          )}
        </div>
      </div>
    </div>
  );
};
