import React, { useState } from 'react';
import { X, Plus, Minus, MessageSquareQuote } from 'lucide-react';
import type { MenuItem } from '../types/api';
import { formatRupiah } from '../utils/format';

interface ItemCustomizeModalProps {
  item: MenuItem | null;
  initialQuantity?: number;
  initialNotes?: string;
  onClose: () => void;
  onConfirm: (item: MenuItem, quantity: number, notes: string) => void;
}

export const ItemCustomizeModal: React.FC<ItemCustomizeModalProps> = ({
  item,
  initialQuantity = 1,
  initialNotes = '',
  onClose,
  onConfirm,
}) => {
  const [quantity, setQuantity] = useState(initialQuantity);
  const [notes, setNotes] = useState(initialNotes);

  if (!item) return null;

  const quickNotes = [
    'Level 1',
    'Level 2',
    'Level 3',
    'Tanpa Cabe',
    'Jangan pakai seledri',
    'Es sedikit',
    'Dipisah sambalnya',
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-stone-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-t-3xl sm:rounded-3xl p-5 shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-stone-100">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-orange-600 bg-orange-50 px-2 py-0.5 rounded-full">
              {item.category}
            </span>
            <h2 className="text-lg font-extrabold text-stone-900 mt-1">{item.name}</h2>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-stone-100 text-stone-500 hover:bg-stone-200 flex items-center justify-center cursor-pointer transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {item.description && (
          <p className="text-xs text-stone-500 mt-3">{item.description}</p>
        )}

        {/* Notes Input */}
        <div className="mt-4">
          <label className="text-xs font-bold text-stone-700 flex items-center gap-1.5 mb-1.5">
            <MessageSquareQuote className="w-3.5 h-3.5 text-orange-500" />
            <span>Catatan Khusus (opsional)</span>
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Contoh: Level 2, jangan terlalu pedas, kuah dipisah..."
            rows={2}
            className="w-full text-xs p-3 rounded-xl border border-stone-200 focus:outline-hidden focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 resize-none"
          />

          {/* Quick Note Pills */}
          <div className="flex flex-wrap gap-1.5 mt-2">
            {quickNotes.map((quick) => (
              <button
                key={quick}
                type="button"
                onClick={() => setNotes((prev) => (prev ? `${prev}, ${quick}` : quick))}
                className="text-[11px] font-medium bg-stone-100 hover:bg-orange-100 text-stone-600 hover:text-orange-800 px-2.5 py-1 rounded-lg cursor-pointer transition"
              >
                + {quick}
              </button>
            ))}
          </div>
        </div>

        {/* Quantity Stepper */}
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-stone-100">
          <div>
            <span className="text-xs text-stone-400 block font-medium">Subtotal</span>
            <span className="text-base font-extrabold text-orange-600">
              {formatRupiah(item.price * quantity)}
            </span>
          </div>

          <div className="flex items-center gap-3 bg-stone-100 rounded-2xl p-1">
            <button
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
              disabled={quantity <= 1}
              className="w-8 h-8 rounded-xl bg-white text-stone-700 disabled:opacity-40 flex items-center justify-center font-bold shadow-xs cursor-pointer active:scale-95 transition"
            >
              <Minus className="w-4 h-4" />
            </button>
            <span className="text-sm font-extrabold text-stone-900 min-w-5 text-center">
              {quantity}
            </span>
            <button
              onClick={() => setQuantity((q) => q + 1)}
              className="w-8 h-8 rounded-xl bg-orange-600 text-white flex items-center justify-center font-bold shadow-xs cursor-pointer active:scale-95 transition"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Confirm Button */}
        <button
          onClick={() => onConfirm(item, quantity, notes)}
          className="w-full mt-4 py-3 bg-orange-600 hover:bg-orange-700 active:scale-98 text-white font-bold rounded-2xl shadow-lg shadow-orange-600/30 flex items-center justify-center gap-2 cursor-pointer transition text-sm"
        >
          <span>Tambahkan ke Pesanan</span>
          <span>•</span>
          <span>{formatRupiah(item.price * quantity)}</span>
        </button>
      </div>
    </div>
  );
};
