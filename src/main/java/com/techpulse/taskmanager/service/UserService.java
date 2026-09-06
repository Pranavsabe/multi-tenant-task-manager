package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.request.CreateUserRequest;
import com.techpulse.taskmanager.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    List<UserResponse> getAllUsersForCurrentTenant();
}
