package com.techpulse.taskmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResponseEntity<ApiError> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Task not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Task not found");
    }

    @Test
    void handleDuplicateResource_returns409() {
        ResponseEntity<ApiError> response = handler.handleDuplicateResource(
                new DuplicateResourceException("already exists"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalidCredentials_returns401() {
        ResponseEntity<ApiError> response = handler.handleInvalidCredentials(
                new InvalidCredentialsException("bad creds"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleTenantAccessDenied_returns403() {
        ResponseEntity<ApiError> response = handler.handleTenantAccessDenied(
                new TenantAccessDeniedException("cross tenant blocked"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(
                new IllegalArgumentException("bad arg"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<ApiError> response = handler.handleGenericException(
                new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
