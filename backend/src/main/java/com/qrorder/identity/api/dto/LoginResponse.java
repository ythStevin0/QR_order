package com.qrorder.identity.api.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String displayName,
        List<StaffRoleDto> roles
) {
    public static LoginResponse of(String token, String username, String displayName, List<StaffRoleDto> roles) {
        return new LoginResponse(token, "Bearer", username, displayName, roles);
    }
}
