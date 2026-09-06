package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.LoginRequest;
import com.techpulse.taskmanager.dto.response.AuthResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.InvalidCredentialsException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Tenant tenant;
    private User user;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("acme").build();
        user = User.builder().id(10L).email("admin@acme.com").password("hashed").role(Role.ADMIN).tenantId(1L).build();
        request = new LoginRequest("acme", "admin@acme.com", "plainPass");
    }

    @Test
    void login_success_returnsTokenWithTenantAndRole() {
        when(tenantRepository.findByName("acme")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("admin@acme.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPass", "hashed")).thenReturn(true);
        when(jwtService.generateToken(10L, "admin@acme.com", 1L, "ADMIN")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getTenantId()).isEqualTo(1L);
    }

    @Test
    void login_unknownTenant_throwsInvalidCredentials() {
        when(tenantRepository.findByName("acme")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(tenantRepository.findByName("acme")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(tenantRepository.findByName("acme")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("admin@acme.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
