import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/theme/app_theme.dart';
import '../providers/kitchen_provider.dart';
import '../widgets/order_card.dart';

class KitchenScreen extends ConsumerStatefulWidget {
  const KitchenScreen({super.key});

  @override
  ConsumerState<KitchenScreen> createState() => _KitchenScreenState();
}

class _KitchenScreenState extends ConsumerState<KitchenScreen> {
  void _showSettingsDialog() {
    final controller = TextEditingController(text: ApiClient().dio.options.baseUrl);
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Pengaturan API Backend'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Gunakan 10.0.2.2 untuk Android Emulator, localhost untuk Desktop/Web, atau IP WiFi laptop Anda jika menggunakan HP fisik.',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: controller,
              decoration: const InputDecoration(
                labelText: 'Base URL Backend',
                border: OutlineInputBorder(),
                hintText: 'http://10.0.2.2:8080/api/v1',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Batal'),
          ),
          ElevatedButton(
            onPressed: () {
              ApiClient().updateBaseUrl(controller.text.trim());
              ref.read(kitchenProvider.notifier).loadOrders();
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Base URL diubah ke: ${controller.text.trim()}')),
              );
            },
            child: const Text('Simpan'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(kitchenProvider);
    final notifier = ref.read(kitchenProvider.notifier);

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F4),
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Colors.green,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 8),
                const Text(
                  'Layar Dapur (KDS)',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                ),
              ],
            ),
            Text(
              'Outlet Utama • Auto-Sync Aktif',
              style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            tooltip: 'Pengaturan IP Backend',
            onPressed: _showSettingsDialog,
          ),
          IconButton(
            icon: state.isLoading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh),
            tooltip: 'Segarkan Pesanan',
            onPressed: () => notifier.loadOrders(),
          ),
          const SizedBox(width: 8),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                _buildFilterChip(
                  label: 'Semua',
                  count: state.orders.length,
                  filterKey: 'ALL',
                  isActive: state.activeFilter == 'ALL',
                  onTap: () => notifier.setFilter('ALL'),
                ),
                const SizedBox(width: 8),
                _buildFilterChip(
                  label: 'Menunggu Masak',
                  count: state.countByStatus('PAID'),
                  filterKey: 'PAID',
                  color: AppTheme.statusPaid,
                  isActive: state.activeFilter == 'PAID',
                  onTap: () => notifier.setFilter('PAID'),
                ),
                const SizedBox(width: 8),
                _buildFilterChip(
                  label: 'Sedang Dimasak',
                  count: state.countByStatus('PREPARING'),
                  filterKey: 'PREPARING',
                  color: AppTheme.statusPreparing,
                  isActive: state.activeFilter == 'PREPARING',
                  onTap: () => notifier.setFilter('PREPARING'),
                ),
                const SizedBox(width: 8),
                _buildFilterChip(
                  label: 'Siap Saji',
                  count: state.countByStatus('READY_FOR_PICKUP'),
                  filterKey: 'READY_FOR_PICKUP',
                  color: AppTheme.statusReady,
                  isActive: state.activeFilter == 'READY_FOR_PICKUP',
                  onTap: () => notifier.setFilter('READY_FOR_PICKUP'),
                ),
              ],
            ),
          ),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: () => notifier.loadOrders(),
        child: _buildBody(state, notifier),
      ),
    );
  }

  Widget _buildFilterChip({
    required String label,
    required int count,
    required String filterKey,
    required bool isActive,
    required VoidCallback onTap,
    Color? color,
  }) {
    final chipColor = color ?? AppTheme.primaryOrange;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: isActive ? chipColor : Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isActive ? chipColor : Colors.grey.shade300,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: TextStyle(
                color: isActive ? Colors.white : const Color(0xFF1C1917),
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(width: 6),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: isActive ? Colors.white.withAlpha(50) : Colors.grey.shade200,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                '$count',
                style: TextStyle(
                  color: isActive ? Colors.white : Colors.grey.shade800,
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBody(KitchenState state, KitchenNotifier notifier) {
    if (state.errorMessage != null && state.orders.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.cloud_off, size: 48, color: Colors.grey),
              const SizedBox(height: 12),
              const Text(
                'Tidak dapat tersambung ke backend',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              const SizedBox(height: 6),
              Text(
                state.errorMessage!,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () => notifier.loadOrders(),
                icon: const Icon(Icons.refresh),
                label: const Text('Coba Lagi'),
              ),
            ],
          ),
        ),
      );
    }

    final orders = state.filteredOrders;

    if (orders.isEmpty) {
      return LayoutBuilder(
        builder: (context, constraints) => SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: SizedBox(
            height: constraints.maxHeight,
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade200,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.soup_kitchen_outlined, size: 48, color: Colors.grey.shade600),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Dapur Bersih & Tidak Ada Antrean',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF1C1917),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Pesanan baru yang telah dibayar akan muncul di sini secara otomatis.',
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final crossAxisCount = constraints.maxWidth >= 900
            ? 3
            : constraints.maxWidth >= 600
                ? 2
                : 1;

        return GridView.builder(
          padding: const EdgeInsets.all(16),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: 16,
            mainAxisSpacing: 16,
            mainAxisExtent: 380,
          ),
          itemCount: orders.length,
          itemBuilder: (context, index) {
            final order = orders[index];
            return KitchenOrderCard(
              order: order,
              onStartCooking: () => notifier.startCooking(order.id),
              onMarkReady: () => notifier.markReady(order.id),
              onMarkCompleted: () => notifier.markCompleted(order.id),
            );
          },
        );
      },
    );
  }
}
