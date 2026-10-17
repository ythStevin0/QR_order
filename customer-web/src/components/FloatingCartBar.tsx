import React from 'react';
import { ShoppingCart, ArrowRight } from 'lucide-react';
import { formatRupiah } from '../utils/format';

interface FloatingCartBarProps {
  totalItems: number;
  subtotal: number;
  onOpenCart: () => void;
}

export const FloatingCartBar: React.FC<FloatingCartBarProps> = ({
  totalItems,
  subtotal,
  onOpenCart,
}) => {
  if (totalItems === 0) return null;

  return (
    <div className="fixed bottom-0 inset-x-0 z-40 p-3 sm:p-4 pointer-events-none">
      <div className="max-w-md mx-auto pointer-events-auto">
        <button
          onClick={onOpenCart}
          className="w-full bg-linear-to-r from-orange-600 to-amber-500 hover:from-orange-700 hover:to-amber-600 text-white rounded-2xl p-3.5 shadow-xl shadow-orange-600/30 flex items-center justify-between cursor-pointer transition active:scale-98 group"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-xs flex items-center justify-center font-extrabold text-sm relative">
              <ShoppingCart className="w-5 h-5 text-white" />
              <span className="absolute -top-1.5 -right-1.5 bg-stone-900 text-white text-[10px] font-extrabold w-5 h-5 rounded-full flex items-center justify-center border-2 border-orange-500 shadow-xs">
                {totalItems}
              </span>
            </div>
            <div className="text-left">
              <span className="text-[11px] text-orange-100 font-semibold block leading-tight">
                {totalItems} Menu Dipilih
              </span>
              <span className="text-base font-extrabold tracking-tight">
                {formatRupiah(subtotal)}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-1.5 bg-white/20 px-3 py-1.5 rounded-xl font-bold text-xs backdrop-blur-xs group-hover:bg-white/30 transition">
            <span>Lihat Pesanan</span>
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
          </div>
        </button>
      </div>
    </div>
  );
};
