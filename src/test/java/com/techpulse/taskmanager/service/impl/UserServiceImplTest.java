package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.CreateUserRequest;
import com.techpulse.taskmanager.dto.response.UserResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createUser_success_usesTenantIdFromContextNotRequest() {
        CreateUserRequest request = new CreateUserRequest("user@acme.com", "pass1234", Role.USER);
        when(userRepository.existsByEmailAndTenantId("user@acme.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void createUser_duplicateEmailInTenant_throwsException() {
        CreateUserRequest request = new CreateUserRequest("user@acme.com", "pass1234", Role.USER);
        when(userRepository.existsByEmailAndTenantId("user@acme.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllUsersForCurrentTenant_returnsOnlyTenantScopedUsers() {
        User u1 = User.builder().id(1L).email("a@acme.com").role(Role.USER).tenantId(1L).build();
        User u2 = User.builder().id(2L).email("b@acme.com").role(Role.MANAGER).tenantId(1L).build();
        when(userRepository.findAllByTenantId(1L)).thenReturn(List.of(u1, u2));

        List<UserResponse> result = userService.getAllUsersForCurrentTenant();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.getTenantId().equals(1L));
    }
}
