package com.qrorder.identity.application;

import com.qrorder.identity.api.dto.LoginRequest;
import com.qrorder.identity.api.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
