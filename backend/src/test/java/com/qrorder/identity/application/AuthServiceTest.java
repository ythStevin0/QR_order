package com.qrorder.identity.application;

import com.qrorder.identity.api.dto.LoginRequest;
import com.qrorder.identity.api.dto.LoginResponse;
import com.qrorder.identity.domain.StaffOutletRole;
import com.qrorder.identity.domain.StaffRole;
import com.qrorder.identity.domain.StaffUser;
import com.qrorder.identity.infrastructure.StaffOutletRoleRepository;
import com.qrorder.identity.infrastructure.StaffUserRepository;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.shared.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StaffUserRepository staffUserRepository;

    @Mock
    private StaffOutletRoleRepository staffOutletRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "super-secure-dev-secret-key-that-is-at-least-256-bits-long-12345678",
                3600000
        );
        authService = new AuthServiceImpl(
                staffUserRepository,
                staffOutletRoleRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void login_withValidCredentials_returnsTokenAndRoles() {
        StaffUser user = new StaffUser("kitchen1", "$2a$10$hashedPassword", "Staf Dapur");
        ReflectionTestUtils.setField(user, "id", 10L);

        Outlet outlet = org.mockito.Mockito.mock(Outlet.class);
        when(outlet.getId()).thenReturn(1L);

        StaffOutletRole role = new StaffOutletRole(user, outlet, StaffRole.KITCHEN);

        when(staffUserRepository.findByUsername("kitchen1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(staffOutletRoleRepository.findByStaffUserId(10L)).thenReturn(List.of(role));

        LoginResponse response = authService.login(new LoginRequest("kitchen1", "password123"));

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("kitchen1");
        assertThat(response.displayName()).isEqualTo("Staf Dapur");
        assertThat(response.roles()).hasSize(1);
        assertThat(response.roles().get(0).role()).isEqualTo(StaffRole.KITCHEN);
        assertThat(response.roles().get(0).outletId()).isEqualTo(1L);

        // Verify token validity with JwtService
        assertThat(jwtService.isTokenValid(response.token())).isTrue();
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("kitchen1");
    }

    @Test
    void login_withInvalidPassword_throwsUnauthorizedException() {
        StaffUser user = new StaffUser("kitchen1", "$2a$10$hashedPassword", "Staf Dapur");

        when(staffUserRepository.findByUsername("kitchen1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("kitchen1", "wrongpass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_withUnknownUsername_throwsUnauthorizedException() {
        when(staffUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "any")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
