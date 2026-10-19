class KitchenOrderItem {
  final int id;
  final int menuItemId;
  final String itemName;
  final int unitPrice;
  final int quantity;
  final int subtotal;
  final String? notes;

  KitchenOrderItem({
    required this.id,
    required this.menuItemId,
    required this.itemName,
    required this.unitPrice,
    required this.quantity,
    required this.subtotal,
    this.notes,
  });

  factory KitchenOrderItem.fromJson(Map<String, dynamic> json) {
    return KitchenOrderItem(
      id: json['id'] as int? ?? 0,
      menuItemId: json['menuItemId'] as int? ?? 0,
      itemName: json['itemName'] as String? ?? 'Item',
      unitPrice: json['unitPrice'] as int? ?? 0,
      quantity: json['quantity'] as int? ?? 1,
      subtotal: json['subtotal'] as int? ?? 0,
      notes: json['notes'] as String?,
    );
  }
}

class KitchenOrder {
  final String id;
  final int outletId;
  final String orderType; // DINE_IN, TAKE_AWAY
  final String? tableNumber;
  final int? queueNumber;
  final String? customerName;
  final int subtotal;
  final int taxAmount;
  final int serviceCharge;
  final int total;
  final String status; // PAID, PREPARING, READY_FOR_PICKUP, COMPLETED, CANCELLED
  final String? notes;
  final int version;
  final DateTime createdAt;
  final List<KitchenOrderItem> items;

  KitchenOrder({
    required this.id,
    required this.outletId,
    required this.orderType,
    this.tableNumber,
    this.queueNumber,
    this.customerName,
    required this.subtotal,
    required this.taxAmount,
    required this.serviceCharge,
    required this.total,
    required this.status,
    this.notes,
    required this.version,
    required this.createdAt,
    required this.items,
  });

  factory KitchenOrder.fromJson(Map<String, dynamic> json) {
    var rawItems = json['items'] as List<dynamic>? ?? [];
    List<KitchenOrderItem> itemsList =
        rawItems.map((i) => KitchenOrderItem.fromJson(i as Map<String, dynamic>)).toList();

    return KitchenOrder(
      id: json['id'] as String? ?? '',
      outletId: json['outletId'] as int? ?? 1,
      orderType: json['orderType'] as String? ?? 'DINE_IN',
      tableNumber: json['tableNumber'] as String?,
      queueNumber: json['queueNumber'] as int?,
      customerName: json['customerName'] as String?,
      subtotal: json['subtotal'] as int? ?? 0,
      taxAmount: json['taxAmount'] as int? ?? 0,
      serviceCharge: json['serviceCharge'] as int? ?? 0,
      total: json['total'] as int? ?? 0,
      status: json['status'] as String? ?? 'PAID',
      notes: json['notes'] as String?,
      version: json['version'] as int? ?? 1,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String) ?? DateTime.now()
          : DateTime.now(),
      items: itemsList,
    );
  }

  bool get isDineIn => orderType == 'DINE_IN';
  bool get isTakeAway => orderType == 'TAKE_AWAY';

  String get displayDestination {
    if (isDineIn) {
      return 'Meja ${tableNumber ?? "-"}';
    } else {
      final queueStr = queueNumber != null ? '#$queueNumber' : '';
      final nameStr = customerName != null && customerName!.isNotEmpty ? ' ($customerName)' : '';
      return 'Take Away $queueStr$nameStr';
    }
  }
}
