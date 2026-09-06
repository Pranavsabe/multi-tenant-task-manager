package com.techpulse.taskmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegisterRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    private String tenantName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String adminPassword;
}
