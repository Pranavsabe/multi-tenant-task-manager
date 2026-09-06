package com.techpulse.taskmanager.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGetTenantId_worksCorrectly() {
        TenantContext.setTenantId(42L);
        assertThat(TenantContext.getTenantId()).isEqualTo(42L);
    }

    @Test
    void setAndGetUserId_worksCorrectly() {
        TenantContext.setUserId(7L);
        assertThat(TenantContext.getUserId()).isEqualTo(7L);
    }

    @Test
    void setAndGetRole_worksCorrectly() {
        TenantContext.setRole("ADMIN");
        assertThat(TenantContext.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void clear_removesAllValues() {
        TenantContext.setTenantId(1L);
        TenantContext.setUserId(2L);
        TenantContext.setRole("USER");

        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(TenantContext.getUserId()).isNull();
        assertThat(TenantContext.getRole()).isNull();
    }
}
