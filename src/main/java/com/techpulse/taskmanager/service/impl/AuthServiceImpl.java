package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.LoginRequest;
import com.techpulse.taskmanager.dto.response.AuthResponse;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.InvalidCredentialsException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse login(LoginRequest request) {
        
        Tenant tenant = tenantRepository.findByName(request.getTenantName())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid tenant, email or password"));

        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid tenant, email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid tenant, email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), tenant.getId(),
                user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenantId(tenant.getId())
                .build();
    }
}
