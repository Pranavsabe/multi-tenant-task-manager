package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.response.TenantResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TenantResponse registerTenant(TenantRegisterRequest request) {
        if (tenantRepository.existsByName(request.getTenantName())) {
            throw new DuplicateResourceException(
                    "A tenant with name '" + request.getTenantName() + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .build();
        tenant = tenantRepository.save(tenant);

        User admin = User.builder()
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role(Role.ADMIN)
                .tenantId(tenant.getId())
                .build();
        userRepository.save(admin);

        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
