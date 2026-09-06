package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.response.TenantResponse;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private TenantRegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new TenantRegisterRequest("acme", "admin@acme.com", "secret123");
    }

    @Test
    void registerTenant_success_createsTenantAndAdminUser() {
        when(tenantRepository.existsByName("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        TenantResponse response = tenantService.registerTenant(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("acme");

        ArgumentCaptor<com.techpulse.taskmanager.entity.User> captor =
                ArgumentCaptor.forClass(com.techpulse.taskmanager.entity.User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getRole().name()).isEqualTo("ADMIN");
    }

    @Test
    void registerTenant_duplicateName_throwsException() {
        when(tenantRepository.existsByName("acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.registerTenant(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }
}
