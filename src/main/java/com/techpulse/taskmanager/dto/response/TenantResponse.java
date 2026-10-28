package com.techpulse.taskmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@DatA
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
