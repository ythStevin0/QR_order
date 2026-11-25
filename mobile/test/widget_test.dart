import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_order_app/main.dart';
import 'package:qr_order_app/features/kitchen/models/kitchen_order.dart';
import 'package:qr_order_app/features/kitchen/providers/kitchen_provider.dart';

class TestKitchenNotifier extends KitchenNotifier {
  final KitchenState initialState;

  TestKitchenNotifier([this.initialState = const KitchenState(isLoading: false)]);

  @override
  KitchenState build() {
    return initialState;
  }

  @override
  void startAutoPolling() {
    // Disable timer in tests
  }
}

KitchenOrder _buildOrder({
  required String id,
  required String status,
  String? notes,
  int itemCount = 1,
}) {
  return KitchenOrder(
    id: id,
    outletId: 1,
    orderType: 'TAKE_AWAY',
    queueNumber: 7,
    customerName: 'Budi',
    subtotal: 15000 * itemCount,
    taxAmount: 1500 * itemCount,
    serviceCharge: 750 * itemCount,
    total: 17250 * itemCount,
    status: status,
    notes: notes,
    version: 1,
    createdAt: DateTime.now(),
    items: List.generate(
      itemCount,
      (i) => KitchenOrderItem(
        id: i + 1,
        menuItemId: i + 1,
        itemName: 'Menu Item ${i + 1}',
        unitPrice: 15000,
        quantity: 1,
        subtotal: 15000,
        notes: i == 0 ? 'Level 2' : null,
      ),
    ),
  );
}

void main() {
  testWidgets('Kitchen App smoke test and online indicator', (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          kitchenProvider.overrideWith(() => TestKitchenNotifier()),
        ],
        child: const QrOrderKitchenApp(),
      ),
    );

    expect(find.byType(QrOrderKitchenApp), findsOneWidget);
    expect(find.text('Layar Dapur (KDS)'), findsOneWidget);
    expect(find.text('Outlet Utama • Terhubung (Auto-Sync)'), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('Kitchen App renders PAID, PREPARING, and READY orders without overflow', (WidgetTester tester) async {
    final orders = [
      _buildOrder(id: 'o1', status: 'PAID', notes: 'Pisah sambal', itemCount: 6),
      _buildOrder(id: 'o2', status: 'PREPARING'),
      _buildOrder(id: 'o3', status: 'READY'),
    ];

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          kitchenProvider.overrideWith(
            () => TestKitchenNotifier(KitchenState(orders: orders, isLoading: false)),
          ),
        ],
        child: const QrOrderKitchenApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Mulai Masak'), findsOneWidget);
    expect(find.text('Tandai Siap Saji'), findsOneWidget);
    expect(find.text('Selesai Diantar'), findsOneWidget);
    expect(find.text('Catatan Pesanan: Pisah sambal'), findsOneWidget);

    await tester.tap(find.text('Siap Saji'));
    await tester.pumpAndSettle();
    expect(find.text('Selesai Diantar'), findsOneWidget);
    expect(find.text('Mulai Masak'), findsNothing);

    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('Kitchen App shows offline banner when disconnected with cached orders', (WidgetTester tester) async {
    final orders = [_buildOrder(id: 'o1', status: 'PAID')];

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          kitchenProvider.overrideWith(
            () => TestKitchenNotifier(KitchenState(orders: orders, isOffline: true, isLoading: false)),
          ),
        ],
        child: const QrOrderKitchenApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Outlet Utama • Terputus dari Server'), findsOneWidget);
    expect(
      find.text('Koneksi ke server terputus — mencoba menyambung ulang secara otomatis...'),
      findsOneWidget,
    );

    await tester.pumpWidget(const SizedBox.shrink());
  });
}
