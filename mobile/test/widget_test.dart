import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_order_app/main.dart';
import 'package:qr_order_app/features/kitchen/providers/kitchen_provider.dart';

class TestKitchenNotifier extends KitchenNotifier {
  @override
  KitchenState build() {
    return const KitchenState(isLoading: false);
  }

  @override
  void startAutoPolling() {
    // Disable timer in tests
  }
}

void main() {
  testWidgets('Kitchen App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          kitchenProvider.overrideWith(TestKitchenNotifier.new),
        ],
        child: const QrOrderKitchenApp(),
      ),
    );

    expect(find.byType(QrOrderKitchenApp), findsOneWidget);
    expect(find.text('Layar Dapur (KDS)'), findsOneWidget);

    // Unmount to cleanly dispose
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
