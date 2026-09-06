package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.request.LoginRequest;
import com.techpulse.taskmanager.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
