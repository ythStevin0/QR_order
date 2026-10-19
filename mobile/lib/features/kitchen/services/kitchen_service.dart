import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';
import '../models/kitchen_order.dart';

class KitchenService {
  final Dio _dio = ApiClient().dio;

  /// Fetch all active kitchen orders (status PAID, PREPARING, READY_FOR_PICKUP)
  Future<List<KitchenOrder>> getActiveKitchenOrders(int outletId) async {
    try {
      final response = await _dio.get(
        '/kitchen/orders',
        queryParameters: {'outletId': outletId},
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'] as List<dynamic>? ?? [];
        return data.map((json) => KitchenOrder.fromJson(json as Map<String, dynamic>)).toList();
      }
      return [];
    } on DioException catch (e) {
      throw Exception('Gagal memuat pesanan dapur: ${e.message}');
    }
  }

  /// Update order status (e.g. PAID -> PREPARING, PREPARING -> READY_FOR_PICKUP)
  Future<KitchenOrder> updateOrderStatus({
    required String orderId,
    required String status,
    String staffUsername = 'KITCHEN_STAFF',
  }) async {
    try {
      final response = await _dio.patch(
        '/orders/$orderId/status',
        data: {'status': status},
        options: Options(
          headers: {'X-Staff-Username': staffUsername},
        ),
      );

      if (response.statusCode == 200 && response.data != null) {
        return KitchenOrder.fromJson(response.data['data'] as Map<String, dynamic>);
      }
      throw Exception('Gagal memperbarui status pesanan');
    } on DioException catch (e) {
      throw Exception('Error update status: ${e.message}');
    }
  }
}
