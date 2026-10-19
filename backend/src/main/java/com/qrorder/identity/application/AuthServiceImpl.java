package com.qrorder.identity.application;

import com.qrorder.identity.api.dto.LoginRequest;
import com.qrorder.identity.api.dto.LoginResponse;
import com.qrorder.identity.api.dto.StaffRoleDto;
import com.qrorder.identity.domain.StaffOutletRole;
import com.qrorder.identity.domain.StaffUser;
import com.qrorder.identity.infrastructure.StaffOutletRoleRepository;
import com.qrorder.identity.infrastructure.StaffUserRepository;
import com.qrorder.shared.error.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final StaffUserRepository staffUserRepository;
    private final StaffOutletRoleRepository staffOutletRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            StaffUserRepository staffUserRepository,
            StaffOutletRoleRepository staffOutletRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.staffUserRepository = staffUserRepository;
        this.staffOutletRoleRepository = staffOutletRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.username());

        StaffUser user = staffUserRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found [{}]", request.username());
                    return new UnauthorizedException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password [{}]", request.username());
            throw new UnauthorizedException("Invalid username or password");
        }

        List<StaffOutletRole> outletRoles = staffOutletRoleRepository.findByStaffUserId(user.getId());
        List<StaffRoleDto> roleDtos = outletRoles.stream()
                .map(r -> new StaffRoleDto(r.getOutlet().getId(), r.getRole()))
                .toList();

        List<String> roleNames = outletRoles.stream()
                .map(r -> r.getRole().name())
                .distinct()
                .toList();

        String token = jwtService.generateToken(user.getId(), user.getUsername(), roleNames);
        log.info("Login successful for user: {} with roles: {}", user.getUsername(), roleNames);

        return LoginResponse.of(token, user.getUsername(), user.getDisplayName(), roleDtos);
    }
}
