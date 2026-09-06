  package com.techpulse.taskmanager.controller;

import com.techpulse.taskmanager.dto.request.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.response.TenantResponse;
import com.techpulse.taskmanager.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<TenantResponse> register(@Valid @RequestBody TenantRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.registerTenant(request));
    }
}
