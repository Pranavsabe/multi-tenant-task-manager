package com.techpulse.taskmanager.repository;

import com.techpulse.taskmanager.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsByName(String name);
    Optional<Tenant> findByName(String name);
}
