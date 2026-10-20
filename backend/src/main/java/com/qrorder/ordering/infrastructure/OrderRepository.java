package com.qrorder.ordering.infrastructure;

import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    // Keyset / Cursor Pagination (Topik 13 & 18): O(log N) composite index seek
    @Query("""
        SELECT DISTINCT o FROM Order o 
        LEFT JOIN FETCH o.lines 
        WHERE o.outlet.id = :outletId 
        ORDER BY o.createdAt DESC, o.id DESC
    """)
    List<Order> findFirstKeysetPage(
            @Param("outletId") Long outletId,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT o FROM Order o 
        LEFT JOIN FETCH o.lines 
        WHERE o.outlet.id = :outletId 
          AND (o.createdAt < :cursorCreatedAt OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId))
        ORDER BY o.createdAt DESC, o.id DESC
    """)
    List<Order> findNextKeysetPage(
            @Param("outletId") Long outletId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
