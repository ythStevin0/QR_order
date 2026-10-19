import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../models/kitchen_order.dart';
import '../services/kitchen_service.dart';

class KitchenState {
  final List<KitchenOrder> orders;
  final bool isLoading;
  final String? errorMessage;
  final String activeFilter; // 'ALL', 'PAID', 'PREPARING', 'READY_FOR_PICKUP'
  final DateTime? lastUpdated;

  const KitchenState({
    this.orders = const [],
    this.isLoading = false,
    this.errorMessage,
    this.activeFilter = 'ALL',
    this.lastUpdated,
  });

  KitchenState copyWith({
    List<KitchenOrder>? orders,
    bool? isLoading,
    String? errorMessage,
    String? activeFilter,
    DateTime? lastUpdated,
  }) {
    return KitchenState(
      orders: orders ?? this.orders,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      activeFilter: activeFilter ?? this.activeFilter,
      lastUpdated: lastUpdated ?? this.lastUpdated,
    );
  }

  List<KitchenOrder> get filteredOrders {
    if (activeFilter == 'ALL') {
      return orders;
    }
    return orders.where((o) => o.status == activeFilter).toList();
  }

  int countByStatus(String status) {
    return orders.where((o) => o.status == status).length;
  }
}

class KitchenNotifier extends Notifier<KitchenState> {
  Timer? _pollingTimer;

  @override
  KitchenState build() {
    ref.onDispose(() {
      _pollingTimer?.cancel();
    });

    // Initial load and timer setup
    Future.microtask(() {
      loadOrders();
      startAutoPolling();
    });

    return const KitchenState(isLoading: true);
  }

  KitchenService get _service => ref.read(kitchenServiceProvider);

  void startAutoPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(const Duration(seconds: 4), (_) {
      loadOrders(silent: true);
    });
  }

  void stopAutoPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  Future<void> loadOrders({bool silent = false}) async {
    if (!silent) {
      state = state.copyWith(isLoading: true, errorMessage: null);
    }
    try {
      final orders = await _service.getActiveKitchenOrders(ApiConstants.defaultOutletId);
      state = state.copyWith(
        orders: orders,
        isLoading: false,
        errorMessage: null,
        lastUpdated: DateTime.now(),
      );
    } catch (e) {
      if (!silent) {
        state = state.copyWith(
          isLoading: false,
          errorMessage: e.toString(),
        );
      }
    }
  }

  void setFilter(String filter) {
    state = state.copyWith(activeFilter: filter);
  }

  Future<void> startCooking(String orderId) async {
    try {
      final updated = await _service.updateOrderStatus(
        orderId: orderId,
        status: 'PREPARING',
      );
      final updatedList = state.orders.map((o) => o.id == orderId ? updated : o).toList();
      state = state.copyWith(orders: updatedList);
    } catch (e) {
      state = state.copyWith(errorMessage: 'Gagal mengubah ke Sedang Dimasak: $e');
    }
  }

  Future<void> markReady(String orderId) async {
    try {
      final updated = await _service.updateOrderStatus(
        orderId: orderId,
        status: 'READY_FOR_PICKUP',
      );
      final updatedList = state.orders.map((o) => o.id == orderId ? updated : o).toList();
      state = state.copyWith(orders: updatedList);
    } catch (e) {
      state = state.copyWith(errorMessage: 'Gagal mengubah ke Siap Saji: $e');
    }
  }

  Future<void> markCompleted(String orderId) async {
    try {
      await _service.updateOrderStatus(
        orderId: orderId,
        status: 'COMPLETED',
      );
      // Remove from active list
      final updatedList = state.orders.where((o) => o.id != orderId).toList();
      state = state.copyWith(orders: updatedList);
    } catch (e) {
      state = state.copyWith(errorMessage: 'Gagal menyelesaikan pesanan: $e');
    }
  }
}

final kitchenServiceProvider = Provider<KitchenService>((ref) => KitchenService());

final kitchenProvider = NotifierProvider<KitchenNotifier, KitchenState>(KitchenNotifier.new);
