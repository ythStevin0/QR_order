package com.qrorder.ordering.infrastructure;

import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.lines LEFT JOIN FETCH o.outlet WHERE o.outlet.id = :outletId AND o.status IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findKitchenQueue(
            @Param("outletId") Long outletId,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    List<Order> findByOutletIdOrderByCreatedAtDesc(Long outletId);
}
