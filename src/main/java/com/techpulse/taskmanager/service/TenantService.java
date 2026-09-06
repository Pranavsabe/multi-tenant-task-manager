package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.request.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.response.TenantResponse;

public interface TenantService {
    TenantResponse registerTenant(TenantRegisterRequest request);
}
