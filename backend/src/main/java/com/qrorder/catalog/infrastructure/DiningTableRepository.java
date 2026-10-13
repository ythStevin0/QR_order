package com.qrorder.catalog.infrastructure;

import com.qrorder.catalog.domain.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    Optional<DiningTable> findByOutletIdAndTableNumber(Long outletId, String tableNumber);
    List<DiningTable> findByOutletIdAndActiveTrue(Long outletId);
}
