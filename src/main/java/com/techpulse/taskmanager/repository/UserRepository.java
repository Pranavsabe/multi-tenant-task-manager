package com.techpulse.taskmanager.repository;

import com.techpulse.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    
    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);
}
