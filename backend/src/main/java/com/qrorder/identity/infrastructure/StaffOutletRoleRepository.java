package com.qrorder.identity.infrastructure;

import com.qrorder.identity.domain.StaffOutletRole;
import com.qrorder.identity.domain.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffOutletRoleRepository extends JpaRepository<StaffOutletRole, Long> {
    List<StaffOutletRole> findByStaffUserId(Long staffUserId);
    Optional<StaffOutletRole> findByStaffUserIdAndOutletIdAndRole(Long staffUserId, Long outletId, StaffRole role);
}
