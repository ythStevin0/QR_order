package com.qrorder.catalog.infrastructure;

import com.qrorder.catalog.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByOutletIdAndAvailableTrue(Long outletId);
    List<MenuItem> findByOutletId(Long outletId);
}
