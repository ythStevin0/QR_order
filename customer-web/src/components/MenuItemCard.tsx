import React from 'react';
import { Plus, Minus, Utensils, Coffee, Sparkles } from 'lucide-react';
import type { MenuItem } from '../types/api';
import { formatRupiah } from '../utils/format';

interface MenuItemCardProps {
  item: MenuItem;
  cartQuantity: number;
  onAddToCart: (item: MenuItem) => void;
  onUpdateQuantity: (item: MenuItem, delta: number) => void;
}

export const MenuItemCard: React.FC<MenuItemCardProps> = ({
  item,
  cartQuantity,
  onAddToCart,
  onUpdateQuantity,
}) => {
  const isDrink = item.category?.toLowerCase().includes('minuman') || item.category?.toLowerCase().includes('drink');

  return (
    <div
      className={`bg-white rounded-2xl p-3.5 border transition-all duration-200 flex gap-3 relative overflow-hidden ${
        item.isAvailable
          ? 'border-stone-200/80 shadow-xs hover:border-orange-300 hover:shadow-md'
          : 'border-stone-200 bg-stone-50/70 opacity-75'
      }`}
    >
      {/* Visual Placeholder / Image */}
      <div className="w-24 h-24 rounded-xl bg-linear-to-tr from-amber-100 via-orange-50 to-orange-100 flex items-center justify-center shrink-0 border border-orange-200/50 relative overflow-hidden">
        {isDrink ? (
          <Coffee className="w-9 h-9 text-orange-400 stroke-[1.5]" />
        ) : (
          <Utensils className="w-9 h-9 text-orange-500 stroke-[1.5]" />
        )}
        
        {/* Subtle decorative sparkle */}
        <Sparkles className="w-3.5 h-3.5 text-amber-300 absolute top-2 right-2 opacity-60" />

        {!item.isAvailable && (
          <div className="absolute inset-0 bg-stone-900/60 backdrop-blur-[1px] flex items-center justify-center">
            <span className="text-[10px] font-extrabold text-white bg-red-600 px-2 py-0.5 rounded-full uppercase tracking-wider">
              Habis
            </span>
          </div>
        )}
      </div>

      {/* Details */}
      <div className="flex-1 flex flex-col justify-between min-w-0">
        <div>
          <div className="flex items-start justify-between gap-1">
            <h3 className="font-bold text-sm text-stone-900 leading-tight truncate">
              {item.name}
            </h3>
          </div>
          {item.description && (
            <p className="text-xs text-stone-500 mt-1 line-clamp-2 leading-relaxed">
              {item.description}
            </p>
          )}
        </div>

        <div className="flex items-center justify-between mt-2 pt-1 border-t border-stone-100">
          <span className="text-sm font-extrabold text-orange-600">
            {formatRupiah(item.price)}
          </span>

          {item.isAvailable && (
            <div>
              {cartQuantity === 0 ? (
                <button
                  onClick={() => onAddToCart(item)}
                  className="px-3 py-1.5 bg-orange-600 hover:bg-orange-700 active:scale-95 text-white font-bold text-xs rounded-xl flex items-center gap-1 shadow-xs shadow-orange-600/30 cursor-pointer transition"
                >
                  <Plus className="w-3.5 h-3.5 stroke-[2.5]" />
                  <span>Tambah</span>
                </button>
              ) : (
                <div className="flex items-center gap-2 bg-orange-50 border border-orange-200 rounded-xl px-1.5 py-0.5">
                  <button
                    onClick={() => onUpdateQuantity(item, -1)}
                    className="w-6 h-6 rounded-lg bg-white text-orange-700 flex items-center justify-center font-bold text-xs shadow-2xs hover:bg-orange-100 active:scale-90 cursor-pointer transition"
                    aria-label="Kurangi"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="text-xs font-extrabold text-orange-900 min-w-3.5 text-center">
                    {cartQuantity}
                  </span>
                  <button
                    onClick={() => onUpdateQuantity(item, 1)}
                    className="w-6 h-6 rounded-lg bg-orange-600 text-white flex items-center justify-center font-bold text-xs shadow-2xs hover:bg-orange-700 active:scale-90 cursor-pointer transition"
                    aria-label="Tambah"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
