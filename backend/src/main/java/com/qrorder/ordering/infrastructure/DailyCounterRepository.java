package com.qrorder.ordering.infrastructure;

import com.qrorder.ordering.domain.DailyCounter;
import com.qrorder.ordering.domain.DailyCounterId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyCounterRepository extends JpaRepository<DailyCounter, DailyCounterId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyCounter d WHERE d.outletId = :outletId AND d.counterDate = :counterDate")
    Optional<DailyCounter> findByOutletIdAndCounterDateWithLock(
            @Param("outletId") Long outletId,
            @Param("counterDate") LocalDate counterDate
    );
}
