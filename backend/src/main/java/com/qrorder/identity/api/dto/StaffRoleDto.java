package com.qrorder.identity.api.dto;

import com.qrorder.identity.domain.StaffRole;

public record StaffRoleDto(
        Long outletId,
        StaffRole role
) {}
