import React from 'react';
import { UtensilsCrossed, ShoppingBag, Store, AlertCircle, Clock } from 'lucide-react';
import type { TableContext } from '../types/api';

interface HeaderProps {
  context: TableContext | null;
  orderType: 'DINE_IN' | 'TAKE_AWAY';
  activeOrderId?: string | null;
  onViewActiveOrder?: () => void;
  isDemo?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  context,
  orderType,
  activeOrderId,
  onViewActiveOrder,
  isDemo = false,
}) => {
  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-stone-200/80 shadow-xs">
      {/* Demo Mode Notice */}
      {isDemo && (
        <div className="bg-amber-400 text-amber-950 px-4 py-1 text-[11px] font-bold flex items-center justify-center gap-1.5 shadow-xs">
          <span>⚡ Mode Demo Aktif (Backend belum aktif di localhost:8080)</span>
        </div>
      )}

      {/* Top Banner if outlet is closed */}
      {context && !context.outletIsOpen && (
        <div className="bg-red-500 text-white px-4 py-2 text-xs font-semibold flex items-center justify-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>Outlet sedang TUTUP. Anda belum dapat melakukan pemesanan saat ini.</span>
        </div>
      )}

      {/* Active Order Resume Alert */}
      {activeOrderId && (
        <div className="bg-orange-600 text-white px-4 py-2 text-xs font-medium flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 shrink-0 animate-spin" style={{ animationDuration: '3s' }} />
            <span>Anda memiliki pesanan aktif yang sedang diproses</span>
          </div>
          <button
            onClick={onViewActiveOrder}
            className="px-2.5 py-1 bg-white text-orange-700 font-bold rounded-lg text-xs hover:bg-orange-50 transition active:scale-95 cursor-pointer shadow-xs"
          >
            Lihat Status
          </button>
        </div>
      )}

      <div className="max-w-md mx-auto px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-linear-to-tr from-orange-500 to-amber-400 flex items-center justify-center text-white shadow-md shadow-orange-500/20">
            <UtensilsCrossed className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-stone-900 tracking-tight leading-tight flex items-center gap-1.5">
              {context ? context.outletName : 'Memuat Outlet...'}
              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 ring-2 ring-emerald-100" />
            </h1>
            <p className="text-xs text-stone-500 flex items-center gap-1 font-medium">
              <Store className="w-3.5 h-3.5" />
              <span>Self-Service QR Order</span>
            </p>
          </div>
        </div>

        {/* Badge Dine In / Take Away */}
        <div>
          {orderType === 'DINE_IN' ? (
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-bold shadow-xs">
              <UtensilsCrossed className="w-3.5 h-3.5 text-emerald-600" />
              <span>Meja {context?.tableNumber || '-'}</span>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl text-xs font-bold shadow-xs">
              <ShoppingBag className="w-3.5 h-3.5 text-blue-600" />
              <span>Take Away</span>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
