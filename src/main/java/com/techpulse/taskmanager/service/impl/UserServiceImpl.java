package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.CreateUserRequest;
import com.techpulse.taskmanager.dto.response.UserResponse;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // tenant_id is derived ONLY from the JWT-backed TenantContext -
        // never from the request body, exactly as mandated.
        Long tenantId = TenantContext.getTenantId();

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists in this tenant");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .tenantId(tenantId)
                .build();

        user = userRepository.save(user);

        return toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsersForCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();
        return userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .build();
    }
}
