import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/network/api_client.dart';
import '../models/kitchen_order.dart';
import '../services/kitchen_service.dart';

class KitchenState {
  final List<KitchenOrder> orders;
  final bool isLoading;
  final bool isOffline;
  final String? errorMessage;
  final String? actionError;
  final String activeFilter; // 'ALL', 'PAID', 'PREPARING', 'READY'
  final DateTime? lastUpdated;

  const KitchenState({
    this.orders = const [],
    this.isLoading = false,
    this.isOffline = false,
    this.errorMessage,
    this.actionError,
    this.activeFilter = 'ALL',
    this.lastUpdated,
  });

  KitchenState copyWith({
    List<KitchenOrder>? orders,
    bool? isLoading,
    bool? isOffline,
    String? errorMessage,
    bool clearError = false,
    String? actionError,
    bool clearActionError = false,
    String? activeFilter,
    DateTime? lastUpdated,
  }) {
    return KitchenState(
      orders: orders ?? this.orders,
      isLoading: isLoading ?? this.isLoading,
      isOffline: isOffline ?? this.isOffline,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      actionError:
          clearActionError ? null : (actionError ?? this.actionError),
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
  static const _baseUrlPrefKey = 'kitchen_api_base_url';
  Timer? _pollingTimer;

  @override
  KitchenState build() {
    ref.onDispose(() {
      _pollingTimer?.cancel();
    });

    // Restore custom API base URL if saved, then load orders and start polling
    Future.microtask(() async {
      await _restoreBaseUrl();
      await loadOrders();
      startAutoPolling();
    });

    return const KitchenState(isLoading: true);
  }

  KitchenService get _service => ref.read(kitchenServiceProvider);

  Future<void> _restoreBaseUrl() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedUrl = prefs.getString(_baseUrlPrefKey);
      if (savedUrl != null && savedUrl.trim().isNotEmpty) {
        ApiClient().updateBaseUrl(savedUrl.trim());
      }
    } catch (_) {
      // Ignore preference read issues (e.g. in isolated test environments)
    }
  }

  Future<void> updateBaseUrl(String url) async {
    final trimmed = url.trim();
    if (trimmed.isEmpty) return;
    ApiClient().updateBaseUrl(trimmed);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_baseUrlPrefKey, trimmed);
    } catch (_) {
      // Ignore preference write issues
    }
    await loadOrders();
  }

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

  void clearActionError() {
    if (state.actionError != null) {
      state = state.copyWith(clearActionError: true);
    }
  }

  Future<void> loadOrders({bool silent = false}) async {
    if (!silent) {
      state = state.copyWith(isLoading: true, clearError: true);
    }
    try {
      final orders = await _service.getActiveKitchenOrders(ApiConstants.defaultOutletId);
      state = state.copyWith(
        orders: orders,
        isLoading: false,
        isOffline: false,
        clearError: true,
        lastUpdated: DateTime.now(),
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        isOffline: true,
        errorMessage: (!silent || state.orders.isEmpty) ? e.toString() : null,
      );
    }
  }

  void setFilter(String filter) {
    state = state.copyWith(activeFilter: filter);
  }

  Future<void> startCooking(String orderId) async {
    state = state.copyWith(clearActionError: true);
    try {
      final updated = await _service.updateOrderStatus(
        orderId: orderId,
        status: 'PREPARING',
      );
      final updatedList = state.orders.map((o) => o.id == orderId ? updated : o).toList();
      state = state.copyWith(orders: updatedList, isOffline: false);
    } catch (e) {
      state = state.copyWith(actionError: 'Gagal mengubah ke Sedang Dimasak: $e');
    }
  }

  Future<void> markReady(String orderId) async {
    state = state.copyWith(clearActionError: true);
    try {
      final updated = await _service.updateOrderStatus(
        orderId: orderId,
        status: 'READY',
      );
      final updatedList = state.orders.map((o) => o.id == orderId ? updated : o).toList();
      state = state.copyWith(orders: updatedList, isOffline: false);
    } catch (e) {
      state = state.copyWith(actionError: 'Gagal mengubah ke Siap Saji: $e');
    }
  }

  Future<void> markCompleted(String orderId) async {
    state = state.copyWith(clearActionError: true);
    try {
      await _service.updateOrderStatus(
        orderId: orderId,
        status: 'COMPLETED',
      );
      // Remove from active list
      final updatedList = state.orders.where((o) => o.id != orderId).toList();
      state = state.copyWith(orders: updatedList, isOffline: false);
    } catch (e) {
      state = state.copyWith(actionError: 'Gagal menyelesaikan pesanan: $e');
    }
  }
}

final kitchenServiceProvider = Provider<KitchenService>((ref) => KitchenService());

final kitchenProvider = NotifierProvider<KitchenNotifier, KitchenState>(KitchenNotifier.new);
